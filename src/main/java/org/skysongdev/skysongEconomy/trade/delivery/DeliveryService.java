package org.skysongdev.skysongEconomy.trade.delivery;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.trade.DeliveryTime;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.ImportOffer;
import org.skysongdev.skysongEconomy.trade.ImportQuote;
import org.skysongdev.skysongEconomy.trade.Ledger;
import org.skysongdev.skysongEconomy.trade.LedgerSelections;
import org.skysongdev.skysongEconomy.trade.TradePricing;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/**
 * Orders imports into delivery slots, and keeps every delivery until it is collected.
 * <p>
 * Ordering needs Economy and Logistics, the ledger selected, the owner or a group allowed to
 * order imports, and a free delivery slot. The orderer's own balance pays, and orders can't be
 * cancelled. Deliveries stay when their ledger is removed or the business loses slots; only new
 * orders wait for a free slot. A deleted business refunds every delivery to whoever paid for it.
 * Kept in memory and saved right away; use it on the main thread.
 */
public final class DeliveryService implements BusinessChangeListener {
    /** What ordering needs from the rest of the plugin. */
    public record Dependencies(EconomyLogisticsSettings settings, LedgerSelections selections, AccessService access,
                               TradePricing pricing, Wallet wallet) {
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
     * @throws BusinessException when ordering isn't allowed now or the character can't pay
     * @throws org.skysongdev.skysongEconomy.prices.PriceException when the import can't be priced
     */
    public Delivery order(Business business, CharacterRef character, Ledger ledger, ImportOffer offer, DeliveryTime time) {
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
        ImportQuote quote = deps.pricing().quote(business, ledger, offer, time);
        if (!deps.wallet().withdraw(character.player(), quote.price().price())) {
            throw new BusinessException("business.imports.cannot-afford",
                    Map.of("price", deps.wallet().format(quote.price().price())));
        }
        Instant now = clock.instant();
        Delivery delivery = new Delivery(UUID.randomUUID().toString(), business.id(), ledger.id(), offer.id(), time.id(),
                offer.contents().roll(random), character, quote.price().price(), now, now.plus(time.duration()));
        byBusiness.computeIfAbsent(business.id(), id -> new ArrayList<>()).add(delivery);
        repository.save(delivery);
        return delivery;
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
