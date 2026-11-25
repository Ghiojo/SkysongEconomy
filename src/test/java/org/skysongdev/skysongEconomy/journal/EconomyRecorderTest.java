package org.skysongdev.skysongEconomy.journal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.business.BusinessInfo;
import org.skysongdev.skysongEconomy.api.business.Businesses;
import org.skysongdev.skysongEconomy.api.business.NpcUpkeepInfo;
import org.skysongdev.skysongEconomy.api.business.ShiftInfo;
import org.skysongdev.skysongEconomy.api.business.TrainingInfo;
import org.skysongdev.skysongEconomy.api.claims.AppliedModifier;
import org.skysongdev.skysongEconomy.api.claims.ClaimSale;
import org.skysongdev.skysongEconomy.api.claims.Seller;
import org.skysongdev.skysongEconomy.api.items.ItemCount;
import org.skysongdev.skysongEconomy.api.journal.EconomyRecord;
import org.skysongdev.skysongEconomy.api.journal.ImportRoll;
import org.skysongdev.skysongEconomy.api.journal.JournalIcDate;
import org.skysongdev.skysongEconomy.api.journal.JournalQuery;
import org.skysongdev.skysongEconomy.api.journal.MoneyAccount;
import org.skysongdev.skysongEconomy.api.journal.MoneyMovement;
import org.skysongdev.skysongEconomy.api.journal.RecordItem;
import org.skysongdev.skysongEconomy.api.journal.RecordKind;
import org.skysongdev.skysongEconomy.api.nodes.BoosterInfo;
import org.skysongdev.skysongEconomy.api.nodes.HarvestCostInfo;
import org.skysongdev.skysongEconomy.api.nodes.NodeInfo;
import org.skysongdev.skysongEconomy.api.nodes.PlantingChange;
import org.skysongdev.skysongEconomy.api.piers.Pier;
import org.skysongdev.skysongEconomy.api.piers.PierDetails;
import org.skysongdev.skysongEconomy.api.piers.Shipment;
import org.skysongdev.skysongEconomy.api.piers.ShipmentItem;
import org.skysongdev.skysongEconomy.api.trade.ExportSale;
import org.skysongdev.skysongEconomy.api.trade.LedgerSelectionChange;
import org.skysongdev.skysongEconomy.api.trade.TradePrice;
import org.skysongdev.skysongEconomy.calendar.CalendarClock;
import org.skysongdev.skysongEconomy.calendar.CalendarDate;
import org.skysongdev.skysongEconomy.calendar.CalendarDay;
import org.skysongdev.skysongEconomy.calendar.CalendarNames;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyRecorderTest {
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    private static final CharacterRef ANNA = new CharacterRef(UUID.nameUUIDFromBytes("anna".getBytes()), "Anna");
    private static final CharacterRef BRAM = new CharacterRef(UUID.nameUUIDFromBytes("bram".getBytes()), "Bram");
    private static final BusinessInfo TAVERN = new BusinessInfo("tavern", "trading", "<gold>Tavern", 2,
            Optional.of(ANNA), List.of(BRAM));
    private static final BusinessInfo MARKET = new BusinessInfo("market", "trading", "Market", 1, Optional.empty(),
            List.of());
    private static final Pier HARBOUR = new Pier("harbour", new PierDetails("<aqua>Harbour Pier", "OAK_BOAT", List.of()),
            true, Map.of(), BigDecimal.TEN, List.of(), Pier.CONFIG);

    private final InMemoryJournal saved = new InMemoryJournal();
    private final List<EconomyRecord> announced = new ArrayList<>();
    private final TestCalendar calendar = new TestCalendar();
    private EconomyRecorder recorder;

    @BeforeEach
    void setUp() {
        saved.last = 41;
        recorder = new EconomyRecorder(new EconomyRecorder.Dependencies(saved, announced::add, calendar,
                Optional.of(new OneBusiness(TAVERN)), new Names(), Clock.fixed(NOW, ZoneOffset.UTC)));
        recorder.loadSequence();
    }

    private static Shipment shipment() {
        return new Shipment("s1", "tavern", "smugglers", "wine", "slow", Optional.of("harbour"),
                List.of(new ShipmentItem("red_wine", 4)), BRAM, new BigDecimal("120"), NOW, NOW.plusSeconds(60),
                Set.of("contraband"));
    }

    private static MoneyMovement move(MoneyAccount from, MoneyAccount to, String amount) {
        return new MoneyMovement(from, to, new BigDecimal(amount));
    }

    @Test
    void recordsAreNumberedOnFromTheLastSavedOneAnnouncedThenSaved() {
        EconomyRecord first = recorder.vaultCollected(TAVERN, ANNA.player(), new BigDecimal("30"));
        EconomyRecord second = recorder.vaultCollected(TAVERN, ANNA.player(), new BigDecimal("5"));

        assertEquals(42, first.sequence());
        assertEquals(43, second.sequence());
        assertEquals(43, recorder.latestSequence());
        assertEquals(List.of(first, second), announced);
        assertEquals(List.of(first, second), saved.records);
        assertEquals(NOW, first.at());
    }

    @Test
    void recordsCarryTheIcDateAndWeekOrNoneWithoutACalendar() {
        calendar.date = Optional.of(new CalendarDate(1203, "Idalia", 3, 47, false));
        EconomyRecord monthDay = recorder.vaultCollected(TAVERN, ANNA.player(), BigDecimal.ONE);
        calendar.date = Optional.of(new CalendarDate(1203, "Festival", 2, 361, true));
        EconomyRecord festival = recorder.vaultCollected(TAVERN, ANNA.player(), BigDecimal.ONE);
        calendar.date = Optional.empty();
        EconomyRecord none = recorder.vaultCollected(TAVERN, ANNA.player(), BigDecimal.ONE);

        assertEquals(Optional.of(new JournalIcDate(1203, "Idalia", 3, 47, OptionalInt.of(6), false)), monthDay.icDate());
        assertEquals(OptionalInt.empty(), festival.icDate().orElseThrow().week());
        assertTrue(festival.icDate().orElseThrow().festival());
        assertEquals(Optional.empty(), none.icDate());
    }

    @Test
    void anImportOrderIsPaidToTheServerWithItsPriceNamesAndTags() {
        TradePrice price = TradePrice.flat(new BigDecimal("120"));
        EconomyRecord record = recorder.importOrdered(TAVERN, shipment(), Optional.of(HARBOUR), price);

        assertEquals(RecordKind.IMPORT_ORDERED, record.kind());
        assertEquals("s1", record.subjectId());
        assertEquals(Optional.of(BRAM), record.actor());
        assertEquals(Optional.of("<gold>Tavern"), record.businessName());
        assertEquals(List.of(move(MoneyAccount.player(BRAM.player()), MoneyAccount.SERVER, "120")), record.money());
        assertEquals(List.of(new RecordItem("red_wine", 4, RecordItem.Flow.IN)), record.items());
        assertEquals("<red>Smugglers", record.trade().orElseThrow().ledgerName());
        assertEquals("Red wine", record.trade().orElseThrow().offerName());
        assertEquals(Optional.of("<aqua>Harbour Pier"), record.trade().orElseThrow().pierName());
        assertEquals(Optional.of("harbour"), record.pierId());
        assertEquals(Optional.of(price), record.trade().orElseThrow().price());
        assertTrue(record.hasTag("contraband"));
    }

    @Test
    void collectedImportsAreDeliveredInOrLostWhenSeized() {
        ImportRoll roll = new ImportRoll(BigDecimal.TEN, new BigDecimal("3.5"), true, Optional.of("Customs"));
        EconomyRecord seized = recorder.importCollected(new EconomyRecorder.Collection(shipment(), Optional.of(HARBOUR),
                ANNA, true, List.of(), List.of(new ShipmentItem("red_wine", 4)), roll));
        EconomyRecord delivered = recorder.importCollected(new EconomyRecorder.Collection(shipment(), Optional.empty(),
                ANNA, false, List.of(new ShipmentItem("red_wine", 3)), List.of(new ShipmentItem("red_wine", 4)), roll));

        assertEquals(RecordKind.IMPORT_SEIZED, seized.kind());
        assertEquals(List.of(new RecordItem("red_wine", 4, RecordItem.Flow.LOST)), seized.items());
        assertEquals(Optional.of(roll), seized.trade().orElseThrow().roll());
        assertEquals(List.of(), seized.money(), "it was paid when ordered");
        assertEquals(Optional.of("<gold>Tavern"), seized.businessName(), "looked up by id");
        assertEquals(RecordKind.IMPORT_DELIVERED, delivered.kind());
        assertEquals(List.of(new RecordItem("red_wine", 3, RecordItem.Flow.IN)), delivered.items());
    }

    @Test
    void refundsPayThePlayerBackOnlyWhenTheMoneyArrived() {
        EconomyRecord paid = recorder.importRefunded(TAVERN, shipment(), true);
        EconomyRecord failed = recorder.importRefunded(TAVERN, shipment(), false);

        assertEquals(List.of(move(MoneyAccount.SERVER, MoneyAccount.player(BRAM.player()), "120")), paid.money());
        assertEquals(List.of(), failed.money());
        assertEquals(Optional.of("false"), failed.detail("paid-back"));
    }

    @Test
    void anExportSalePaysTheSellerAndTakesTheItems() {
        ExportSale sale = new ExportSale("e1", "tavern", "smugglers", "ale", ANNA, List.of(new ItemCount("ale", 8)),
                TradePrice.flat(new BigDecimal("40")), NOW, Set.of("contraband"));
        EconomyRecord record = recorder.exportSold(TAVERN, sale);

        assertEquals(List.of(move(MoneyAccount.SERVER, MoneyAccount.player(ANNA.player()), "40")), record.money());
        assertEquals(List.of(new RecordItem("ale", 8, RecordItem.Flow.OUT)), record.items());
        assertEquals("ale", record.trade().orElseThrow().offerName(), "an export no longer loaded keeps its id");
        assertEquals(Optional.empty(), record.pierId());
        assertTrue(record.hasTag("contraband"));
    }

    @Test
    void shiftsTrainingAndVaultsMoveMoneyBetweenServerVaultsAndPlayers() {
        ShiftInfo shift = new ShiftInfo(UUID.randomUUID(), "tavern", BRAM, NOW, Duration.ofMinutes(30),
                Duration.ofMinutes(30), new BigDecimal("12"), 2, new BigDecimal("4"), Optional.of(NOW));
        ShiftInfo ownersShift = new ShiftInfo(UUID.randomUUID(), "tavern", ANNA, NOW, Duration.ofMinutes(30),
                Duration.ofMinutes(30), new BigDecimal("12"), 2, BigDecimal.ZERO, Optional.of(NOW));
        TrainingInfo training = new TrainingInfo(UUID.randomUUID(), "tavern", BRAM, NOW, Duration.ofHours(1),
                new BigDecimal("25"));

        assertEquals(List.of(move(MoneyAccount.SERVER, MoneyAccount.vault("tavern"), "4")),
                recorder.shiftEnded(TAVERN, shift).orElseThrow().money());
        assertTrue(recorder.shiftEnded(TAVERN, ownersShift).isEmpty(), "no cut, no record");
        EconomyRecord pay = recorder.shiftPaid(TAVERN, BRAM, List.of(shift), new BigDecimal("12"), 2).orElseThrow();
        assertEquals(List.of(move(MoneyAccount.SERVER, MoneyAccount.player(BRAM.player()), "12")), pay.money());
        assertEquals(Optional.of("2"), pay.detail("tokens"));
        assertTrue(recorder.shiftPaid(TAVERN, BRAM, List.of(shift), BigDecimal.ZERO, 2).isEmpty());
        assertEquals(List.of(move(MoneyAccount.player(BRAM.player()), MoneyAccount.vault("tavern"), "25")),
                recorder.trainingStarted(TAVERN, training).orElseThrow().money());
        assertEquals(List.of(move(MoneyAccount.vault("tavern"), MoneyAccount.player(ANNA.player()), "30")),
                recorder.vaultCollected(TAVERN, ANNA.player(), new BigDecimal("30")).money());
    }

    @Test
    void aClosedVaultGoesToItsOwnerOrLeavesTheEconomy() {
        assertEquals(List.of(move(MoneyAccount.vault("tavern"), MoneyAccount.player(ANNA.player()), "50")),
                recorder.vaultClosed(TAVERN, Optional.of(ANNA.player()), new BigDecimal("50"), true).money());
        assertEquals(List.of(move(MoneyAccount.vault("market"), MoneyAccount.SERVER, "50")),
                recorder.vaultClosed(MARKET, Optional.empty(), new BigDecimal("50"), false).money());
        assertEquals(List.of(move(MoneyAccount.vault("tavern"), MoneyAccount.SERVER, "50")),
                recorder.vaultClosed(TAVERN, Optional.of(ANNA.player()), new BigDecimal("50"), false).money(),
                "a payment that failed left the economy");
    }

    @Test
    void onlyAPaidLedgerRemovalFineIsRecorded() {
        LedgerSelectionChange removed = new LedgerSelectionChange(LedgerSelectionChange.Kind.REMOVED, "tavern",
                "smugglers", new BigDecimal("50"), Optional.of(ANNA), NOW);
        LedgerSelectionChange selected = new LedgerSelectionChange(LedgerSelectionChange.Kind.SELECTED, "tavern",
                "smugglers", BigDecimal.ZERO, Optional.empty(), NOW);

        EconomyRecord fine = recorder.ledgerChanged(TAVERN, removed).orElseThrow();
        assertEquals(RecordKind.LEDGER_FINE, fine.kind());
        assertEquals(List.of(move(MoneyAccount.player(ANNA.player()), MoneyAccount.SERVER, "50")), fine.money());
        assertEquals(Optional.of("<red>Smugglers"), fine.detail("ledger-name"));
        assertTrue(recorder.ledgerChanged(TAVERN, selected).isEmpty());
    }

    @Test
    void harvestsPayTheServerAndBringItemsIn() {
        NodeInfo node = new NodeInfo("mine_1", NodeInfo.RANDOM, "iron", 2, Optional.of("tavern"), Optional.empty());
        EconomyRecord paid = recorder.harvested(node, BRAM, 2, new HarvestCostInfo(1, 0, new BigDecimal("3")),
                List.of(new ItemCount("iron_ore", 5)), Optional.of(new BoosterInfo("mining", "max+1")), Optional.empty());
        EconomyRecord free = recorder.harvested(node, BRAM, 1, new HarvestCostInfo(0, 2, BigDecimal.ZERO),
                List.of(), Optional.empty(), Optional.of("wheat"));

        assertEquals(List.of(move(MoneyAccount.player(BRAM.player()), MoneyAccount.SERVER, "3")), paid.money());
        assertEquals(List.of(new RecordItem("iron_ore", 5, RecordItem.Flow.IN)), paid.items());
        assertEquals(Optional.of("mine_1"), paid.nodeId());
        assertEquals(Optional.of("tavern"), paid.businessId());
        assertEquals(Optional.of("max+1"), paid.detail("booster-effect"));
        assertEquals(List.of(), free.money());
        assertEquals(Optional.of("2"), free.detail("harvest-tokens"));
        assertEquals(Optional.of("wheat"), free.detail("resource"));
    }

    @Test
    void primersGoOutWhenPlantedAndComeBackIn() {
        List<ItemCount> seeds = List.of(new ItemCount("wheat_seeds", 2));
        EconomyRecord planted = recorder.plantingChanged(new PlantingChange(PlantingChange.Kind.PLANTED, "field",
                "wheat", "crop", "tavern", BRAM, BRAM, 0, NOW), seeds);
        EconomyRecord taken = recorder.plantingChanged(new PlantingChange(PlantingChange.Kind.TAKEN_OUT, "field",
                "wheat", "crop", "tavern", BRAM, ANNA, 1, NOW), seeds);

        assertEquals(RecordKind.FARM_PLANTED, planted.kind());
        assertEquals(List.of(new RecordItem("wheat_seeds", 2, RecordItem.Flow.OUT)), planted.items());
        assertEquals(RecordKind.FARM_UNPLANTED, taken.kind());
        assertEquals(List.of(new RecordItem("wheat_seeds", 2, RecordItem.Flow.IN)), taken.items());
        assertEquals(Optional.of(ANNA), taken.actor());
        assertEquals(Optional.of("taken-out"), taken.detail("cause"));
        assertEquals(Optional.of("1"), taken.detail("downtime"));
    }

    @Test
    void upkeepEatsItemsWithNobodyActing() {
        UUID npc = UUID.randomUUID();
        EconomyRecord record = recorder.upkeepPaid(new NpcUpkeepInfo(npc, Optional.of("rations"), false, true,
                NOW.plusSeconds(60), NOW), NOW, List.of(new ItemCount("bread", 2)));

        assertEquals(npc.toString(), record.subjectId());
        assertEquals(Optional.empty(), record.actor());
        assertEquals(List.of(new RecordItem("bread", 2, RecordItem.Flow.OUT)), record.items());
        assertEquals(Optional.of("rations"), record.detail("profile"));
    }

    @Test
    void aResalePaysEachSellerAndModifiersGoToTheServer() {
        Map<UUID, BigDecimal> payouts = new LinkedHashMap<>();
        payouts.put(ANNA.player(), new BigDecimal("600"));
        payouts.put(BRAM.player(), new BigDecimal("400"));
        CharacterRef cara = new CharacterRef(UUID.nameUUIDFromBytes("cara".getBytes()), "Cara");
        ClaimSale resale = new ClaimSale(7, "rose_house", "house", cara,
                Optional.of(new Seller(ANNA, List.of(ANNA, BRAM), payouts)), new BigDecimal("1000"),
                List.of(new AppliedModifier("m1", "district_tax", "claim-type:house", "percent", BigDecimal.TEN,
                        new BigDecimal("100"), "Tax")), new BigDecimal("100"), new BigDecimal("1100"), true, NOW);
        ClaimSale serverSale = new ClaimSale(8, "oak_house", "house", cara, Optional.empty(), new BigDecimal("500"),
                List.of(), BigDecimal.ZERO, new BigDecimal("500"), false, NOW);

        MoneyAccount buyer = MoneyAccount.player(cara.player());
        EconomyRecord resold = recorder.claimBought(resale);
        assertEquals(List.of(move(buyer, MoneyAccount.player(ANNA.player()), "600"),
                move(buyer, MoneyAccount.player(BRAM.player()), "400"), move(buyer, MoneyAccount.SERVER, "100")),
                resold.money());
        assertEquals("claim-sale:7", resold.subjectId());
        assertEquals(Optional.of("rose_house"), resold.claimId());
        assertEquals(List.of(move(buyer, MoneyAccount.SERVER, "500")), recorder.claimBought(serverSale).money());

        EconomyRecord payout = recorder.claimPayout(ANNA.player(), "rose_house", 7, new BigDecimal("600"));
        assertEquals("claim-sale:7", payout.subjectId());
        assertEquals(List.of(), payout.money(), "already counted in the purchase");
        assertEquals(Optional.of(ANNA.player().toString()), payout.detail("account"));
    }

    @Test
    void involvesTheActorAndBothSidesOfAPayment() {
        EconomyRecord collected = recorder.vaultCollected(TAVERN, ANNA.player(), BigDecimal.TEN);
        assertTrue(collected.involves(ANNA.player()));
        assertFalse(collected.involves(BRAM.player()));
        assertFalse(collected.hasTag("contraband"), "not a trade");
    }

    private static final class InMemoryJournal implements JournalRepository {
        final List<EconomyRecord> records = new ArrayList<>();
        long last;

        @Override
        public long lastSequence() {
            return last;
        }

        @Override
        public void save(EconomyRecord record) {
            records.add(record);
        }

        @Override
        public List<EconomyRecord> query(JournalQuery query) {
            return records.stream().filter(query::matches).toList();
        }

        @Override
        public int deleteBefore(Instant time) {
            return 0;
        }
    }

    private static final class TestCalendar implements CalendarClock {
        Optional<CalendarDate> date = Optional.empty();

        @Override
        public Optional<CalendarDay> today() {
            return Optional.empty();
        }

        @Override
        public Optional<CalendarNames> names() {
            return Optional.empty();
        }

        @Override
        public Optional<CalendarDate> date() {
            return date;
        }

        @Override
        public void whenDayChanges(Runnable action) {
        }
    }

    private record OneBusiness(BusinessInfo business) implements Businesses {
        @Override
        public Optional<BusinessInfo> find(String id) {
            return business.id().equals(id) ? Optional.of(business) : Optional.empty();
        }

        @Override
        public List<BusinessInfo> all() {
            return List.of(business);
        }

        @Override
        public List<BusinessInfo> ownedBy(CharacterRef owner) {
            return List.of();
        }
    }

    private static final class Names implements TradeNames {
        @Override
        public Optional<String> ledger(String ledgerId) {
            return ledgerId.equals("smugglers") ? Optional.of("<red>Smugglers") : Optional.empty();
        }

        @Override
        public Optional<String> importOffer(String ledgerId, String importId) {
            return importId.equals("wine") ? Optional.of("Red wine") : Optional.empty();
        }
    }
}
