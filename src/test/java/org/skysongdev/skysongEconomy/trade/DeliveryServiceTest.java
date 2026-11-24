package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.skysongdev.skysongEconomy.api.piers.PierDetails;
import org.skysongdev.skysongEconomy.api.piers.LossModifier;
import org.skysongdev.skysongEconomy.api.piers.Shipment;
import org.skysongdev.skysongEconomy.api.piers.ShipmentItem;
import org.skysongdev.skysongEconomy.api.trade.TradePrice;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.trade.delivery.Delivery;
import org.skysongdev.skysongEconomy.trade.delivery.DeliveryRepository;
import org.skysongdev.skysongEconomy.trade.delivery.DeliveryService;
import org.skysongdev.skysongEconomy.trade.delivery.OrderCancelledException;
import org.skysongdev.skysongEconomy.trade.delivery.ShipmentEvents;
import org.skysongdev.skysongEconomy.trade.pier.InMemoryPierRepository;
import org.skysongdev.skysongEconomy.trade.pier.PierService;
import org.skysongdev.skysongEconomy.trade.pier.PierSettings;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryServiceTest {
    private final TradeFixture trade = new TradeFixture();
    private final InMemoryDeliveries repository = new InMemoryDeliveries();
    private final List<String> warnings = new ArrayList<>();
    private final PierService piers = new PierService(PierSettings.NONE, new InMemoryPierRepository(), warnings::add);
    /** Every shipment ordering reported, and what to answer. */
    private final List<Shipment> ordering = new ArrayList<>();
    private ShipmentEvents.Verdict verdict = ShipmentEvents.Verdict.ALLOW;
    /** The price each ordering was told, every order that went through, and every refund. */
    private final List<TradePrice> prices = new ArrayList<>();
    private final List<Shipment> ordered = new ArrayList<>();
    private final List<Shipment> refunded = new ArrayList<>();
    private final List<Boolean> refundsPaid = new ArrayList<>();
    /** Every arrival reported, what listeners make of it, and every collection reported. */
    private final List<ShipmentEvents.Arrival> arrivals = new ArrayList<>();
    private Function<ShipmentEvents.Arrival, ShipmentEvents.Outcome> listeners = ShipmentEvents.Arrival::asRolled;
    private final List<ShipmentEvents.Outcome> collected = new ArrayList<>();
    private final ShipmentEvents events = new ShipmentEvents() {
        @Override
        public Verdict ordering(Shipment shipment, TradePrice price) {
            ordering.add(shipment);
            prices.add(price);
            return verdict;
        }

        @Override
        public void ordered(Business business, Shipment shipment, TradePrice price) {
            ordered.add(shipment);
        }

        @Override
        public void refunded(Business business, Shipment shipment, boolean paidBack) {
            refunded.add(shipment);
            refundsPaid.add(paidBack);
        }

        @Override
        public Outcome arriving(Arrival arrival) {
            arrivals.add(arrival);
            return listeners.apply(arrival);
        }

        @Override
        public void collected(Arrival arrival, Outcome outcome) {
            collected.add(outcome);
        }
    };
    private DeliveryService deliveries;

    private DeliveryService.Dependencies dependencies() {
        return new DeliveryService.Dependencies(trade.settings, trade.selections, trade.access, trade.pricing,
                trade.wallet, piers, events);
    }

    @BeforeEach
    void setUp() {
        deliveries = new DeliveryService(dependencies(), repository, trade.clock, new Random(3), warnings::add);
        trade.businesses.addListener(deliveries);
        deliveries.loadAll();
        trade.wallet.balances.put(TradeFixture.OWNER.player(), new BigDecimal("500"));
    }

    private Delivery order(CharacterRef character) {
        return deliveries.order(trade.shop, character, TradeFixture.SMUGGLERS, TradeFixture.CRATE, TradeFixture.EXPRESS, null);
    }

    private Delivery orderTo(String pierId) {
        return deliveries.order(trade.shop, TradeFixture.OWNER, TradeFixture.SMUGGLERS, TradeFixture.CRATE,
                TradeFixture.EXPRESS, pierId);
    }

    private static String refusal(Executable action) {
        return assertThrows(BusinessException.class, action).messageKey();
    }

    @Test
    void orderingChargesTheOrdererAndBooksASlot() {
        Delivery delivery = order(TradeFixture.OWNER);
        assertEquals(0, new BigDecimal("444").compareTo(trade.wallet.balance(TradeFixture.OWNER.player())));
        assertEquals(TradeFixture.NOW.plus(Duration.ofHours(24)), delivery.arrivesAt());
        assertFalse(delivery.isReady(TradeFixture.NOW));
        assertTrue(delivery.isReady(delivery.arrivesAt()));
        assertEquals(1, deliveries.freeSlots(trade.shop));
        assertEquals(List.of(delivery), repository.saved);
        int swords = delivery.items().stream().filter(item -> item.materialId().equals("sword")).mapToInt(ItemAmount::amount).sum();
        int spears = delivery.items().stream().filter(item -> item.materialId().equals("spear")).mapToInt(ItemAmount::amount).sum();
        assertEquals(2, swords);
        assertTrue(spears >= 1 && spears <= 3, "rolled " + spears + " spears");
    }

    @Test
    void slotsRunOut() {
        order(TradeFixture.OWNER);
        order(TradeFixture.OWNER);
        assertEquals("business.imports.no-free-slot", refusal(() -> order(TradeFixture.OWNER)));
    }

    @Test
    void onlyTheOwnerAndAllowedGroupsOrder() {
        trade.wallet.balances.put(TradeFixture.WORKER.player(), new BigDecimal("500"));
        assertEquals("business.imports.not-allowed", refusal(() -> order(TradeFixture.WORKER)));
        trade.access.setPermission(trade.shop, AccessGroup.WORKERS, Permission.ORDER_IMPORTS, true);
        order(TradeFixture.WORKER);
        assertEquals(0, new BigDecimal("444").compareTo(trade.wallet.balance(TradeFixture.WORKER.player())));
        assertEquals("business.imports.not-allowed", refusal(() -> order(TradeFixture.STRANGER)));
    }

    @Test
    void refusedWhenTheOrdererCantPay() {
        trade.wallet.balances.put(TradeFixture.OWNER.player(), new BigDecimal("55"));
        assertEquals("business.imports.cannot-afford", refusal(() -> order(TradeFixture.OWNER)));
        assertEquals(2, deliveries.freeSlots(trade.shop));
    }

    @Test
    void theLedgerMustBeSelected() {
        trade.selections.remove(trade.shop, "smugglers");
        assertEquals("business.ledgers.not-selected", refusal(() -> order(TradeFixture.OWNER)));
    }

    @Test
    void removingTheLedgerKeepsDeliveries() {
        order(TradeFixture.OWNER);
        trade.selections.remove(trade.shop, "smugglers");
        assertEquals(1, deliveries.deliveries(trade.shop).size());
    }

    @Test
    void deletingTheBusinessRefundsEveryDelivery() {
        order(TradeFixture.OWNER);
        trade.businesses.delete(trade.shop);
        assertEquals(0, new BigDecimal("500").compareTo(trade.wallet.balance(TradeFixture.OWNER.player())));
        assertTrue(repository.saved.isEmpty());
        assertEquals(List.of(), warnings);
    }

    @Test
    void deliveriesSurviveARestart() {
        Delivery delivery = order(TradeFixture.OWNER);
        deliveries.loadAll();
        assertEquals(List.of(delivery), deliveries.deliveries(trade.shop));
    }

    private DeliveryService at(Instant now) {
        DeliveryService later = new DeliveryService(dependencies(), repository, Clock.fixed(now, ZoneOffset.UTC),
                new Random(3), warnings::add);
        later.loadAll();
        return later;
    }

    @Test
    void collectingHandsOverTheItemsAndFreesTheSlot() {
        Delivery delivery = order(TradeFixture.OWNER);
        assertEquals("business.slots.not-ready", refusal(() -> deliveries.collect(trade.shop, TradeFixture.OWNER, delivery,
                items -> { })));
        DeliveryService later = at(delivery.arrivesAt());
        List<ItemAmount> given = new ArrayList<>();
        later.collect(trade.shop, TradeFixture.OWNER, delivery, given::addAll);
        assertEquals(delivery.items(), given);
        assertEquals(2, later.freeSlots(trade.shop));
        assertTrue(repository.saved.isEmpty());
        assertEquals("business.slots.gone", refusal(() -> later.collect(trade.shop, TradeFixture.OWNER, delivery, items -> { })));
    }

    @Test
    void aFailedHandOverKeepsTheDelivery() {
        Delivery delivery = order(TradeFixture.OWNER);
        DeliveryService later = at(delivery.arrivesAt());
        assertThrows(IllegalStateException.class, () -> later.collect(trade.shop, TradeFixture.OWNER, delivery, items -> {
            throw new IllegalStateException("item file missing");
        }));
        assertEquals(List.of(delivery), later.deliveries(trade.shop));
    }

    @Test
    void theOrdererCanCollectEvenWithoutOrderingRights() {
        trade.access.setPermission(trade.shop, AccessGroup.WORKERS, Permission.ORDER_IMPORTS, true);
        trade.wallet.balances.put(TradeFixture.WORKER.player(), new BigDecimal("500"));
        Delivery delivery = order(TradeFixture.WORKER);
        trade.access.setPermission(trade.shop, AccessGroup.WORKERS, Permission.ORDER_IMPORTS, false);
        DeliveryService later = at(delivery.arrivesAt());
        assertEquals("business.slots.not-allowed", refusal(() -> later.collect(trade.shop, TradeFixture.STRANGER, delivery,
                items -> { })));
        later.collect(trade.shop, TradeFixture.WORKER, delivery, items -> { });
    }

    /** Keeps deliveries in a list. */
    @Test
    void withoutPiersImportsAreOrderedWithoutOne() {
        Delivery delivery = order(TradeFixture.OWNER);

        assertFalse(deliveries.needsPier());
        assertEquals(Optional.empty(), delivery.pierId());
        assertEquals(List.of(delivery.id()), ordering.stream().map(Shipment::id).toList());
    }

    @Test
    void withPiersAnOpenPierMustBeChosen() {
        piers.create("SkysongPolitics", "harbour", new PierDetails("Harbour", "OAK_BOAT", List.of()));
        piers.create("SkysongPolitics", "cove", new PierDetails("Cove", "OAK_BOAT", List.of()));
        piers.setOpen("cove", false);

        assertTrue(deliveries.needsPier());
        assertEquals("business.imports.choose-pier", refusal(() -> order(TradeFixture.OWNER)));
        assertEquals("business.imports.pier-closed", refusal(() -> orderTo("cove")));
        assertEquals("business.imports.pier-gone", refusal(() -> orderTo("moon")));
        assertTrue(deliveries.deliveries(trade.shop).isEmpty());
        assertEquals(0, new BigDecimal("500").compareTo(trade.wallet.balance(TradeFixture.OWNER.player())), "nothing paid");

        Delivery delivery = orderTo("HARBOUR");
        assertEquals(Optional.of("harbour"), delivery.pierId());
        assertEquals(Optional.of("harbour"), ordering.get(0).pierId());
    }

    @Test
    void aCancelledOrderChargesNothingAndBooksNoSlot() {
        verdict = ShipmentEvents.Verdict.cancel("<red>The harbour is blockaded.");

        OrderCancelledException cancelled = assertThrows(OrderCancelledException.class, () -> order(TradeFixture.OWNER));

        assertEquals(Optional.of("<red>The harbour is blockaded."), cancelled.reason());
        assertEquals(0, new BigDecimal("500").compareTo(trade.wallet.balance(TradeFixture.OWNER.player())));
        assertTrue(deliveries.deliveries(trade.shop).isEmpty());
        assertTrue(repository.saved.isEmpty());
    }

    @Test
    void listsWhatIsHeadingToAPier() {
        piers.create("SkysongPolitics", "harbour", new PierDetails("Harbour", "OAK_BOAT", List.of()));
        piers.create("SkysongPolitics", "cove", new PierDetails("Cove", "OAK_BOAT", List.of()));
        piers.connectShipments(deliveries::inbound);
        Delivery first = orderTo("harbour");
        orderTo("cove");

        assertEquals(List.of(first.id()), piers.inbound("Harbour").stream().map(Shipment::id).toList());
        assertEquals(TradeFixture.SMUGGLERS.shipmentTags(TradeFixture.CRATE), piers.inbound("harbour").get(0).tags());
        assertTrue(piers.inbound("moon").isEmpty());
    }

    /** Orders to a new pier with that loss chance, and returns a service an hour after it arrived. */
    private DeliveryService orderedToPierWithChance(String chance) {
        piers.create("SkysongPolitics", "harbour", new PierDetails("Harbour", "OAK_BOAT", List.of()));
        piers.setLossChance("harbour", new BigDecimal(chance));
        Delivery delivery = orderTo("harbour");
        return at(delivery.arrivesAt().plus(Duration.ofHours(1)));
    }

    private Delivery only(DeliveryService service) {
        return service.deliveries(trade.shop).get(0);
    }

    @Test
    void collectingWithoutAPierNeverLosesTheShipment() {
        Delivery delivery = order(TradeFixture.OWNER);
        DeliveryService later = at(delivery.arrivesAt());
        List<ItemAmount> given = new ArrayList<>();

        DeliveryService.Collection collection = later.collect(trade.shop, TradeFixture.OWNER, delivery, given::addAll);

        assertFalse(collection.seized());
        assertEquals(delivery.items(), given);
        assertEquals(0, BigDecimal.ZERO.compareTo(arrivals.get(0).lossChance()));
        assertEquals(Optional.empty(), arrivals.get(0).pier());
        assertEquals(1, collected.size());
    }

    @Test
    void aCertainLossSeizesTheShipmentWithoutARefund() {
        DeliveryService later = orderedToPierWithChance("100");
        Delivery delivery = only(later);
        BigDecimal paid = trade.wallet.balance(TradeFixture.OWNER.player());
        List<ItemAmount> given = new ArrayList<>();

        DeliveryService.Collection collection = later.collect(trade.shop, TradeFixture.OWNER, delivery, given::addAll);

        assertTrue(collection.seized());
        assertTrue(collection.items().isEmpty());
        assertTrue(given.isEmpty(), "nothing is handed over");
        assertTrue(later.deliveries(trade.shop).isEmpty(), "the slot is free");
        assertTrue(repository.saved.isEmpty());
        assertEquals(paid, trade.wallet.balance(TradeFixture.OWNER.player()), "no refund");
        assertTrue(arrivals.get(0).lostByRoll());
        assertTrue(collected.get(0).seized());
    }

    @Test
    void lossModifiersCountWhenTheShipmentArrives() {
        DeliveryService later = orderedToPierWithChance("0");
        piers.setLossModifier("harbour", new LossModifier("guards", new BigDecimal("100"), false));

        assertTrue(later.collect(trade.shop, TradeFixture.OWNER, only(later), items -> { }).seized());
        assertEquals(0, new BigDecimal("100").compareTo(arrivals.get(0).lossChance()));
    }

    @Test
    void listenersCanSaveALostShipment() {
        DeliveryService later = orderedToPierWithChance("100");
        Delivery delivery = only(later);
        listeners = arrival -> new ShipmentEvents.Outcome(false, arrival.items(), Optional.empty());
        List<ItemAmount> given = new ArrayList<>();

        assertFalse(later.collect(trade.shop, TradeFixture.OWNER, delivery, given::addAll).seized());
        assertEquals(delivery.items(), given);
    }

    @Test
    void listenersCanSeizeASafeShipmentWithTheirOwnMessage() {
        DeliveryService later = orderedToPierWithChance("0");
        listeners = arrival -> new ShipmentEvents.Outcome(true, arrival.items(), Optional.of("Caught by the guard"));

        DeliveryService.Collection collection = later.collect(trade.shop, TradeFixture.OWNER, only(later), items -> { });

        assertTrue(collection.seized());
        assertFalse(arrivals.get(0).lostByRoll());
        assertEquals(Optional.of("Caught by the guard"), collection.seizeMessage());
    }

    @Test
    void listenersCanChangeWhatIsHandedOver() {
        DeliveryService later = orderedToPierWithChance("0");
        listeners = arrival -> new ShipmentEvents.Outcome(false, List.of(new ShipmentItem("sword", 1)), Optional.empty());
        List<ItemAmount> given = new ArrayList<>();

        later.collect(trade.shop, TradeFixture.OWNER, only(later), given::addAll);

        assertEquals(List.of(new ItemAmount("sword", 1)), given);
    }

    @Test
    void aShipmentLeftEmptyFreesItsSlotWithoutHandingOverAnything() {
        DeliveryService later = orderedToPierWithChance("0");
        listeners = arrival -> new ShipmentEvents.Outcome(false, List.of(), Optional.empty());
        boolean[] handedOver = {false};

        DeliveryService.Collection collection = later.collect(trade.shop, TradeFixture.OWNER, only(later),
                items -> handedOver[0] = true);

        assertFalse(collection.seized());
        assertFalse(handedOver[0]);
        assertTrue(later.deliveries(trade.shop).isEmpty());
    }

    private static final class InMemoryDeliveries implements DeliveryRepository {
        final List<Delivery> saved = new ArrayList<>();

        @Override
        public List<Delivery> loadAll() {
            return List.copyOf(saved);
        }

        @Override
        public void save(Delivery delivery) {
            saved.add(delivery);
        }

        @Override
        public void delete(String id) {
            saved.removeIf(delivery -> delivery.id().equals(id));
        }
    }

    @Test
    void anOrderThatWentThroughIsReportedWithItsPrice() {
        Delivery delivery = order(TradeFixture.OWNER);

        assertEquals(List.of(delivery.id()), ordered.stream().map(Shipment::id).toList());
        TradePrice price = prices.get(0);
        assertEquals(0, new BigDecimal("56").compareTo(price.paid()));
        assertEquals(0, delivery.price().compareTo(price.paid()));
    }

    @Test
    void refusedOrCancelledOrdersAreNotReportedAsOrdered() {
        trade.wallet.balances.put(TradeFixture.OWNER.player(), new BigDecimal("55"));
        refusal(() -> order(TradeFixture.OWNER));
        verdict = ShipmentEvents.Verdict.cancel(null);
        trade.wallet.balances.put(TradeFixture.OWNER.player(), new BigDecimal("500"));
        assertThrows(OrderCancelledException.class, () -> order(TradeFixture.OWNER));

        assertTrue(ordered.isEmpty());
    }

    @Test
    void deletingTheBusinessReportsEachRefund() {
        Delivery first = order(TradeFixture.OWNER);
        Delivery second = order(TradeFixture.OWNER);
        trade.wallet.refuseDeposits = true;

        trade.businesses.delete(trade.shop);

        assertEquals(List.of(first.id(), second.id()), refunded.stream().map(Shipment::id).toList());
        assertEquals(List.of(false, false), refundsPaid, "the payment back failed");
        assertEquals(2, warnings.size());
    }
}
