package org.skysongdev.skysongEconomy.trade.delivery;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.piers.Pier;
import org.skysongdev.skysongEconomy.api.piers.Piers;
import org.skysongdev.skysongEconomy.api.piers.Shipment;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.trade.DeliveryTime;
import org.skysongdev.skysongEconomy.trade.Durations;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.ImportOffer;
import org.skysongdev.skysongEconomy.trade.ImportQuote;
import org.skysongdev.skysongEconomy.trade.ItemAmount;
import org.skysongdev.skysongEconomy.trade.Ledger;
import org.skysongdev.skysongEconomy.trade.LedgerSelections;
import org.skysongdev.skysongEconomy.trade.TradePricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/**
 * Orders imports into delivery slots, and keeps every delivery until it is collected.
 * <p>
 * Whoever ordered a delivery, the owner, and groups allowed to order imports can collect it once
 * it has arrived; collecting frees its slot. That is when it reaches its pier: the pier's loss
 * chance is rolled, and other plugins may seize it or change what it brings.
 * <p>
 * Ordering needs Economy and Logistics, the ledger selected, the owner or a group allowed to
 * order imports, a free delivery slot and, whenever there are piers, an open receiving pier. Then
 * {@link ShipmentEvents#ordering} lets other plugins stop it. The orderer's own balance pays, and
 * orders can't be cancelled; their pier can't be changed. Deliveries stay when their ledger is removed or the business loses slots; only new
 * orders wait for a free slot. A deleted business refunds every delivery to whoever paid for it.
 * Kept in memory and saved right away; use it on the main thread.
 */
public final class DeliveryService implements BusinessChangeListener {
    /**
     * What ordering needs from the rest of the plugin.
     *
     * @param piers  the receiving piers; with none, imports are ordered without one
     * @param events lets other plugins stop orders
     */
    public record Dependencies(EconomyLogisticsSettings settings, LedgerSelections selections, AccessService access,
                               TradePricing pricing, Wallet wallet, Piers piers, ShipmentEvents events) {
    }

    private final Dependencies deps;
    private final DeliveryRepository repository;
    private final Clock clock;
    private final RandomGenerator random;
    private final Consumer<String> warnings;
    private final Map<String, List<Delivery>> byBusiness = new HashMap<>();

    /**
     * @param random   rolls each delivery's items
     * @param warnings told when a refund for a deleted business fails
     */
    public DeliveryService(Dependencies deps, DeliveryRepository repository, Clock clock, RandomGenerator random,
                           Consumer<String> warnings) {
        this.deps = deps;
        this.repository = repository;
        this.clock = clock;
        this.random = random;
        this.warnings = warnings;
    }

    public void loadAll() {
        byBusiness.clear();
        repository.loadAll().stream()
                .sorted(Comparator.comparing(Delivery::orderedAt))
                .forEach(delivery -> byBusiness.computeIfAbsent(delivery.businessId(), id -> new ArrayList<>()).add(delivery));
    }

    /** @return the business's deliveries, on the way or ready, oldest order first */
    public List<Delivery> deliveries(Business business) {
        return List.copyOf(byBusiness.getOrDefault(business.id(), List.of()));
    }

    /**
     * @return every delivery sent to that pier that hasn't been collected, of every business,
     *         soonest arrival first
     */
    public List<Shipment> inbound(String pierId) {
        return byBusiness.values().stream().flatMap(List::stream)
                .filter(delivery -> delivery.pierId().filter(pierId::equals).isPresent())
                .sorted(Comparator.comparing(Delivery::arrivesAt))
                .map(Shipments::of)
                .toList();
    }

    /** @return whether orders must pick a pier: whenever there is any pier, open or closed */
    public boolean needsPier() {
        return !deps.piers().all().isEmpty();
    }

    /** @return how many more imports the business can order now */
    public int freeSlots(Business business) {
        return Math.max(0, deps.settings().limits(business).imports() - deliveries(business).size());
    }

    /** @return whether the character may order imports there: the owner, or a group allowed to */
    public boolean canOrder(Business business, CharacterRef character) {
        return deps.access().allows(business, character, Permission.ORDER_IMPORTS);
    }

    /**
     * Orders an import: charges the character and books a delivery slot.
     *
     * @param ledger the ledger to order from, which the business must have selected
     * @param pierId the receiving pier; required whenever {@link #needsPier()}, ignored otherwise
     * @throws BusinessException when ordering isn't allowed now, the pier is missing or closed, or
     *                           the character can't pay
     * @throws OrderCancelledException when another plugin cancelled it
     * @throws org.skysongdev.skysongEconomy.prices.PriceException when the import can't be priced
     */
    public Delivery order(Business business, CharacterRef character, Ledger ledger, ImportOffer offer, DeliveryTime time,
                          @Nullable String pierId) {
        if (!deps.settings().enabled(business)) {
            throw new BusinessException("business.ledgers.not-enabled", Map.of("business", business.displayName()));
        }
        if (!deps.selections().isSelected(business, ledger.id())) {
            throw new BusinessException("business.ledgers.not-selected", Map.of("ledger", ledger.id()));
        }
        if (!canOrder(business, character)) {
            throw new BusinessException("business.imports.not-allowed", Map.of("business", business.displayName()));
        }
        if (freeSlots(business) == 0) {
            throw new BusinessException("business.imports.no-free-slot",
                    Map.of("max", String.valueOf(deps.settings().limits(business).imports())));
        }
        Optional<Pier> pier = pier(pierId);
        ImportQuote quote = deps.pricing().quote(business, ledger, offer, time, pier.map(Pier::id).orElse(null));
        Instant now = clock.instant();
        Delivery delivery = new Delivery(UUID.randomUUID().toString(), business.id(), ledger.id(), offer.id(), time.id(),
                offer.contents().roll(random), character, quote.price().price(), now, now.plus(time.duration()),
                pier.map(Pier::id), ledger.shipmentTags(offer));
        ShipmentEvents.Verdict verdict = deps.events().ordering(Shipments.of(delivery));
        if (verdict.cancelled()) {
            throw new OrderCancelledException(verdict.message());
        }
        if (!deps.wallet().withdraw(character.player(), quote.price().price())) {
            throw new BusinessException("business.imports.cannot-afford",
                    Map.of("price", deps.wallet().format(quote.price().price())));
        }
        byBusiness.computeIfAbsent(business.id(), id -> new ArrayList<>()).add(delivery);
        repository.save(delivery);
        return delivery;
    }

    /**
     * @return the open pier an order goes to; empty when there are no piers
     * @throws BusinessException when piers exist and none was picked, or it is gone or closed
     */
    private Optional<Pier> pier(@Nullable String pierId) {
        if (!needsPier()) {
            return Optional.empty();
        }
        if (pierId == null) {
            throw new BusinessException("business.imports.choose-pier");
        }
        Pier pier = deps.piers().find(pierId)
                .orElseThrow(() -> new BusinessException("business.imports.pier-gone", Map.of("pier", pierId)));
        if (!pier.open()) {
            throw new BusinessException("business.imports.pier-closed", Map.of("pier", pier.id()));
        }
        return Optional.of(pier);
    }

    /**
     * @return whether the character may collect that delivery: whoever ordered it, the owner,
     *         or a group allowed to order imports
     */
    public boolean canCollect(Business business, CharacterRef character, Delivery delivery) {
        return delivery.orderedBy().equals(character) || canOrder(business, character);
    }

    /**
     * What collecting a delivery did.
     *
     * @param seized       whether it was lost at its pier, so nothing was handed over
     * @param items        what was handed over; empty when seized
     * @param seizeMessage the text a plugin gave for the seizure; empty for the default
     */
    public record Collection(boolean seized, List<ItemAmount> items, Optional<String> seizeMessage) {
        public Collection {
            items = List.copyOf(items);
            Objects.requireNonNull(seizeMessage, "seizeMessage");
        }
    }

    /**
     * Collects a delivery that has arrived and frees its slot. First its pier's loss chance is
     * rolled and {@link ShipmentEvents#arriving} may change the items or the outcome. A lost
     * (seized) delivery hands nothing over and isn't refunded. If {@code handOver} throws, the
     * delivery stays where it is, so nothing is lost.
     *
     * @param handOver gives the items to the player; not called when the delivery is seized or
     *                 listeners left it empty
     * @throws BusinessException when the delivery isn't there any more, hasn't arrived, or the
     *                           character may not collect it
     */
    public Collection collect(Business business, CharacterRef character, Delivery delivery,
                              Consumer<List<ItemAmount>> handOver) {
        if (!deliveries(business).contains(delivery)) {
            throw new BusinessException("business.slots.gone");
        }
        if (!delivery.isReady(clock.instant())) {
            throw new BusinessException("business.slots.not-ready",
                    Map.of("duration", Durations.format(Duration.between(clock.instant(), delivery.arrivesAt()))));
        }
        if (!canCollect(business, character, delivery)) {
            throw new BusinessException("business.slots.not-allowed", Map.of("business", business.displayName()));
        }
        Optional<Pier> pier = delivery.pierId().flatMap(deps.piers()::find);
        BigDecimal chance = pier.map(found -> found.finalLossChance(delivery.tags())).orElse(BigDecimal.ZERO);
        ShipmentEvents.Arrival arrival = new ShipmentEvents.Arrival(Shipments.of(delivery), pier, character, chance,
                roll(), Shipments.items(delivery.items()));
        ShipmentEvents.Outcome outcome = deps.events().arriving(arrival);
        List<ItemAmount> items = outcome.seized() ? List.of() : outcome.items().stream()
                .map(item -> new ItemAmount(item.materialId(), item.amount())).toList();
        if (!items.isEmpty()) {
            handOver.accept(items);
        }
        byBusiness.get(business.id()).remove(delivery);
        repository.delete(delivery.id());
        deps.events().collected(arrival, outcome);
        return new Collection(outcome.seized(), items, outcome.seizeMessage());
    }

    /** @return a number from 0 (included) to 100 (excluded), with two decimals */
    private BigDecimal roll() {
        return BigDecimal.valueOf(random.nextDouble() * 100).setScale(2, RoundingMode.DOWN);
    }

    /** Refunds every delivery of a deleted business to whoever paid for it. */
    @Override
    public void deleted(Business business) {
        List<Delivery> deliveries = byBusiness.remove(business.id());
        if (deliveries == null) {
            return;
        }
        for (Delivery delivery : deliveries) {
            if (!deps.wallet().deposit(delivery.orderedBy().player(), delivery.price())) {
                warnings.accept("Couldn't refund " + deps.wallet().format(delivery.price()) + " to "
                        + delivery.orderedBy() + " for a delivery of deleted business " + business.id());
            }
            repository.delete(delivery.id());
        }
    }
}
