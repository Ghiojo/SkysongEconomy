package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.InMemoryBusinessRepository;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.money.MapWallet;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerSelectionsTest {
    private static final CharacterRef OWNER = new CharacterRef(UUID.nameUUIDFromBytes("owner".getBytes()), "main");
    private static final CharacterRef WORKER = new CharacterRef(UUID.nameUUIDFromBytes("worker".getBytes()), "main");

    private final MapWallet wallet = new MapWallet();
    private final InMemorySelections repository = new InMemorySelections();
    private BusinessService businesses;
    private LedgerSelections selections;
    private Business shop;

    private static Ledger ledger(String id) {
        return new Ledger(id, id.toUpperCase(), "CHEST", List.of(), Map.of(), Map.of());
    }

    @BeforeEach
    void setUp() {
        BusinessTypeRegistry types = new BusinessTypeRegistry(List.of(
                new BusinessType("trading", "Trading", Set.of("economical", "haggling"), false)));
        BusinessTiers tiers = new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, 3)).toList());
        businesses = new BusinessService(types, tiers, new InMemoryBusinessRepository(), Clock.systemUTC());
        EconomyLogisticsSettings settings = new EconomyLogisticsSettings(Set.of("economical"),
                Map.of(1, new SlotLimits(1, 1), 2, new SlotLimits(2, 2)),
                Map.of("slow", new DeliveryTime("slow", "Slow", Duration.ofDays(7))), new BigDecimal("50"));
        selections = new LedgerSelections(settings, new LedgerRegistry(List.of(ledger("a"), ledger("b"), ledger("c"))),
                repository, wallet, Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC));
        businesses.addListener(selections);
        shop = businesses.create("shop", "trading", OWNER, new BlockPosition("world", 0, 64, 0));
        businesses.setTier(shop, 3);
        businesses.hire(shop, WORKER);
    }

    private static String refusal(Executable action) {
        return assertThrows(BusinessException.class, action).messageKey();
    }

    @Test
    void needsEconomyAndLogistics() {
        assertEquals("business.ledgers.not-enabled", refusal(() -> selections.select(shop, OWNER, "a")));
        businesses.pick(shop, "haggling");
        assertEquals("business.ledgers.not-enabled", refusal(() -> selections.select(shop, OWNER, "a")));
    }

    @Test
    void ownerSelectsUpToTheSlotLimit() {
        businesses.pick(shop, "economical");
        assertEquals(1, selections.freeSlots(shop));
        selections.select(shop, OWNER, "a");
        assertEquals("business.ledgers.full", refusal(() -> selections.select(shop, OWNER, "b")));
        assertEquals("business.ledgers.already-selected", refusal(() -> selections.select(shop, OWNER, "a")));

        businesses.pick(shop, "economical");
        selections.select(shop, OWNER, "b");
        assertEquals(List.of("a", "b"), selections.selected(shop).stream().map(Ledger::id).toList());
        assertEquals(2, repository.saved.size());
    }

    @Test
    void onlyTheOwnerSelectsAndRemoves() {
        businesses.pick(shop, "economical");
        assertEquals("business.not-owner", refusal(() -> selections.select(shop, WORKER, "a")));
        selections.select(shop, OWNER, "a");
        assertEquals("business.not-owner", refusal(() -> selections.remove(shop, WORKER, "a")));
    }

    @Test
    void unknownLedgersAreRefused() {
        businesses.pick(shop, "economical");
        assertEquals("business.ledgers.unknown", refusal(() -> selections.select(shop, OWNER, "zzz")));
        assertEquals("business.ledgers.not-selected", refusal(() -> selections.remove(shop, OWNER, "a")));
    }

    @Test
    void removingChargesTheFineToTheOwner() {
        businesses.pick(shop, "economical");
        selections.select(shop, OWNER, "a");
        wallet.balances.put(OWNER.player(), new BigDecimal("30"));
        assertEquals("business.ledgers.cannot-afford-fine", refusal(() -> selections.remove(shop, OWNER, "a")));
        assertTrue(selections.isSelected(shop, "a"));

        wallet.balances.put(OWNER.player(), new BigDecimal("80"));
        assertEquals(0, new BigDecimal("50").compareTo(selections.remove(shop, OWNER, "a")));
        assertEquals(0, new BigDecimal("30").compareTo(wallet.balance(OWNER.player())));
        assertFalse(selections.isSelected(shop, "a"));
        assertTrue(repository.saved.isEmpty());
    }

    @Test
    void losingSlotsDropsTheNewestSelections() {
        businesses.pick(shop, "economical");
        businesses.pick(shop, "economical");
        selections.select(shop, OWNER, "a");
        selections.select(shop, OWNER, "b");
        businesses.clearPick(shop, 2);
        assertEquals(List.of("a"), selections.selected(shop).stream().map(Ledger::id).toList());
        businesses.clearPick(shop, 1);
        assertEquals(List.of(), selections.selected(shop));
        assertTrue(repository.saved.isEmpty());
    }

    @Test
    void selectionsSurviveARestartAndDeletionClearsThem() {
        businesses.pick(shop, "economical");
        selections.select(shop, OWNER, "a");
        selections.loadAll();
        assertTrue(selections.isSelected(shop, "a"));
        businesses.delete(shop);
        assertTrue(repository.saved.isEmpty());
    }

    @Test
    void ledgersWhoseFileIsGoneTakeNoSlot() {
        repository.saved.add(new SelectedLedger("shop", "removed", Instant.EPOCH));
        selections.loadAll();
        businesses.pick(shop, "economical");
        assertEquals(1, selections.freeSlots(shop));
        assertEquals(List.of(), selections.selected(shop));
    }

    /** Keeps selections in a list. */
    private static final class InMemorySelections implements LedgerSelectionRepository {
        final List<SelectedLedger> saved = new ArrayList<>();

        @Override
        public List<SelectedLedger> loadAll() {
            return List.copyOf(saved);
        }

        @Override
        public void save(SelectedLedger selection) {
            saved.add(selection);
        }

        @Override
        public void delete(String businessId, String ledgerId) {
            saved.removeIf(selection -> selection.businessId().equals(businessId) && selection.ledgerId().equals(ledgerId));
        }
    }
}
