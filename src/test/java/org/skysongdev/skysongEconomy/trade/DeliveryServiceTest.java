package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.trade.delivery.Delivery;
import org.skysongdev.skysongEconomy.trade.delivery.DeliveryRepository;
import org.skysongdev.skysongEconomy.trade.delivery.DeliveryService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryServiceTest {
    private final TradeFixture trade = new TradeFixture();
    private final InMemoryDeliveries repository = new InMemoryDeliveries();
    private final List<String> warnings = new ArrayList<>();
    private DeliveryService deliveries;

    @BeforeEach
    void setUp() {
        deliveries = new DeliveryService(new DeliveryService.Dependencies(trade.settings, trade.selections, trade.access,
                trade.pricing, trade.wallet), repository, trade.clock, new Random(3), warnings::add);
        trade.businesses.addListener(deliveries);
        deliveries.loadAll();
        trade.wallet.balances.put(TradeFixture.OWNER.player(), new BigDecimal("500"));
    }

    private Delivery order(CharacterRef character) {
        return deliveries.order(trade.shop, character, TradeFixture.SMUGGLERS, TradeFixture.CRATE, TradeFixture.EXPRESS);
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
        trade.selections.remove(trade.shop, TradeFixture.OWNER, "smugglers");
        assertEquals("business.ledgers.not-selected", refusal(() -> order(TradeFixture.OWNER)));
    }

    @Test
    void removingTheLedgerKeepsDeliveries() {
        order(TradeFixture.OWNER);
        trade.selections.remove(trade.shop, TradeFixture.OWNER, "smugglers");
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

    /** Keeps deliveries in a list. */
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
}
