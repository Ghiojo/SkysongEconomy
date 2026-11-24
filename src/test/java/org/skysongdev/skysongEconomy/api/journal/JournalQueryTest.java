package org.skysongdev.skysongEconomy.api.journal;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.trade.TradePrice;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalQueryTest {
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    private static final CharacterRef ANNA = new CharacterRef(UUID.nameUUIDFromBytes("anna".getBytes()), "Anna");
    private static final UUID BRAM = UUID.nameUUIDFromBytes("bram".getBytes());

    /** An import ordered by Anna at the tavern, through the harbour, tagged contraband, on IC week 6 of 1203. */
    private static EconomyRecord contrabandImport() {
        TradeDetails trade = new TradeDetails("smugglers", "Smugglers", "wine", "Wine", Optional.of("slow"),
                Optional.of("harbour"), Optional.of("Harbour"), Optional.of(TradePrice.flat(BigDecimal.TEN)),
                Set.of("contraband"), Optional.empty());
        return new EconomyRecord(10, UUID.randomUUID(), RecordKind.IMPORT_ORDERED, "s1", NOW,
                Optional.of(JournalIcDate.of(1203, "Idalia", 3, 47, false)), Optional.of(ANNA), Optional.of("tavern"),
                Optional.of("Tavern"), Optional.of(trade), Optional.empty(), Optional.empty(),
                List.of(new MoneyMovement(MoneyAccount.player(ANNA.player()), MoneyAccount.SERVER, BigDecimal.TEN)),
                List.of(), Map.of());
    }

    /** A Claim bought by Bram on a festival day, paying Anna. */
    private static EconomyRecord festivalClaim() {
        return new EconomyRecord(11, UUID.randomUUID(), RecordKind.CLAIM_BOUGHT, "claim-sale:1", NOW.plusSeconds(10),
                Optional.of(JournalIcDate.of(1203, "Festival", 2, 361, true)), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("rose_house"),
                List.of(new MoneyMovement(MoneyAccount.player(BRAM), MoneyAccount.player(ANNA.player()), BigDecimal.TEN)),
                List.of(), Map.of());
    }

    private static boolean matches(JournalQuery.Builder query, EconomyRecord record) {
        return query.build().matches(record);
    }

    @Test
    void noFiltersMatchEverythingUpToTheDefaultLimitOldestFirst() {
        JournalQuery query = JournalQuery.builder().build();
        assertTrue(query.matches(contrabandImport()));
        assertTrue(query.matches(festivalClaim()));
        assertEquals(JournalQuery.DEFAULT_LIMIT, query.limit());
        assertFalse(query.newestFirst());
    }

    @Test
    void filtersOnKindBusinessPierAndClaim() {
        assertTrue(matches(JournalQuery.builder().kinds(RecordKind.IMPORT_ORDERED, RecordKind.EXPORT_SOLD), contrabandImport()));
        assertFalse(matches(JournalQuery.builder().kinds(RecordKind.EXPORT_SOLD), contrabandImport()));
        assertTrue(matches(JournalQuery.builder().business("tavern").pier("harbour"), contrabandImport()));
        assertFalse(matches(JournalQuery.builder().business("smithy"), contrabandImport()));
        assertFalse(matches(JournalQuery.builder().pier("harbour"), festivalClaim()));
        assertTrue(matches(JournalQuery.builder().claim("rose_house"), festivalClaim()));
    }

    @Test
    void aPlayerMatchesAsActorOrEitherSideOfAPayment() {
        assertTrue(matches(JournalQuery.builder().player(ANNA.player()), contrabandImport()));
        assertTrue(matches(JournalQuery.builder().player(ANNA.player()), festivalClaim()), "Anna was paid");
        assertTrue(matches(JournalQuery.builder().player(BRAM), festivalClaim()), "Bram paid");
        assertFalse(matches(JournalQuery.builder().player(BRAM), contrabandImport()));
    }

    @Test
    void excludingATagKeepsRecordsThatAreNotTrades() {
        assertTrue(matches(JournalQuery.builder().withTag("contraband"), contrabandImport()));
        assertFalse(matches(JournalQuery.builder().withTag("contraband"), festivalClaim()));
        assertFalse(matches(JournalQuery.builder().excludingTag("contraband"), contrabandImport()));
        assertTrue(matches(JournalQuery.builder().excludingTag("contraband"), festivalClaim()));
    }

    @Test
    void realTimeFromIsIncludedAndToIsNot() {
        assertTrue(matches(JournalQuery.builder().from(NOW).to(NOW.plusSeconds(1)), contrabandImport()));
        assertFalse(matches(JournalQuery.builder().to(NOW), contrabandImport()));
        assertTrue(matches(JournalQuery.builder().afterSequence(9), contrabandImport()));
        assertFalse(matches(JournalQuery.builder().afterSequence(10), contrabandImport()));
    }

    @Test
    void icWeeksAndTheFestivalAreSeparateBooks() {
        assertTrue(matches(JournalQuery.builder().icWeek(1203, 6), contrabandImport()));
        assertFalse(matches(JournalQuery.builder().icWeek(1203, 7), contrabandImport()));
        assertFalse(matches(JournalQuery.builder().icWeek(1204, 6), contrabandImport()));
        assertFalse(matches(JournalQuery.builder().icWeek(1203, 6), festivalClaim()));
        assertTrue(matches(JournalQuery.builder().icFestival(1203), festivalClaim()));
        assertFalse(matches(JournalQuery.builder().icFestival(1203), contrabandImport()));
    }

    @Test
    void icRangesIncludeBothEnds() {
        assertTrue(matches(JournalQuery.builder().icFrom(1203, 47).icTo(1203, 47), contrabandImport()));
        assertTrue(matches(JournalQuery.builder().icFrom(1202, 300).icTo(1203, 100), contrabandImport()));
        assertFalse(matches(JournalQuery.builder().icFrom(1203, 48), contrabandImport()));
        assertFalse(matches(JournalQuery.builder().icTo(1203, 46), contrabandImport()));
    }

    @Test
    void recordsWithoutAnIcDateNeverMatchAnIcFilter() {
        EconomyRecord undated = new EconomyRecord(1, UUID.randomUUID(), RecordKind.HARVEST, "node", NOW,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), List.of(), List.of(), Map.of());
        assertFalse(matches(JournalQuery.builder().icFrom(0, 0), undated));
        assertTrue(matches(JournalQuery.builder(), undated));
    }

    @Test
    void refusesLimitsOutsideTheRangeAndNegativeOffsets() {
        assertThrows(IllegalArgumentException.class, () -> JournalQuery.builder().limit(0));
        assertThrows(IllegalArgumentException.class, () -> JournalQuery.builder().limit(JournalQuery.MAX_LIMIT + 1));
        assertThrows(IllegalArgumentException.class, () -> JournalQuery.builder().offset(-1));
        assertThrows(IllegalArgumentException.class, () -> JournalQuery.builder().icWeek(1203, 0));
    }
}
