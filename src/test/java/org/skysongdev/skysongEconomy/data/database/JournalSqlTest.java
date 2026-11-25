package org.skysongdev.skysongEconomy.data.database;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.journal.JournalQuery;
import org.skysongdev.skysongEconomy.api.journal.RecordKind;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JournalSqlTest {
    private static final String SELECT = "SELECT " + JournalSql.COLUMNS + " FROM economy_journal";

    @Test
    void noFiltersReadTheOldestFirstUpToTheLimit() {
        JournalSql.Select select = JournalSql.select(JournalQuery.builder().build());

        assertEquals(SELECT + " ORDER BY sequence ASC LIMIT ? OFFSET ?", select.sql());
        assertEquals(List.of(JournalQuery.DEFAULT_LIMIT, 0), select.parameters());
    }

    @Test
    void everyFilterIsAParameterInTheOrderItAppears() {
        UUID anna = UUID.nameUUIDFromBytes("anna".getBytes());
        Instant from = Instant.ofEpochMilli(1000);
        Instant to = Instant.ofEpochMilli(2000);
        JournalSql.Select select = JournalSql.select(JournalQuery.builder()
                .kinds(RecordKind.EXPORT_SOLD, RecordKind.IMPORT_ORDERED)
                .business("tavern").pier("harbour").player(anna)
                .withTag("wine").excludingTag("contraband")
                .from(from).to(to).afterSequence(40)
                .icWeek(1203, 6).icFrom(1203, 45).icTo(1203, 53)
                .limit(20).offset(40).newestFirst()
                .build());

        assertEquals(SELECT + " WHERE kind IN (?, ?) AND business_id = ? AND pier_id = ?"
                + " AND (actor_uuid = ? OR money LIKE ?) AND tags LIKE ? AND (tags IS NULL OR tags NOT LIKE ?)"
                + " AND recorded_at >= ? AND recorded_at < ? AND sequence > ? AND ic_year = ? AND ic_week = ?"
                + " AND (ic_year > ? OR (ic_year = ? AND ic_day_of_year >= ?))"
                + " AND (ic_year < ? OR (ic_year = ? AND ic_day_of_year <= ?))"
                + " ORDER BY sequence DESC LIMIT ? OFFSET ?", select.sql());
        assertEquals(List.of("IMPORT_ORDERED", "EXPORT_SOLD", "tavern", "harbour", anna.toString(),
                "%\"" + anna + "\"%", "%,wine,%", "%,contraband,%", 1000L, 2000L, 40L, 1203, 6,
                1203, 1203, 45, 1203, 1203, 53, 20, 40), select.parameters());
    }

    @Test
    void theFestivalIsItsOwnFilter() {
        JournalSql.Select select = JournalSql.select(JournalQuery.builder().icFestival(1203).build());

        assertEquals(SELECT + " WHERE ic_year = ? AND ic_festival = ? ORDER BY sequence ASC LIMIT ? OFFSET ?",
                select.sql());
        assertEquals(List.of(1203, true, JournalQuery.DEFAULT_LIMIT, 0), select.parameters());
    }

    @Test
    void tagPatternsMatchWholeTagsWithLikeCharactersTakenLiterally() {
        assertEquals("%,contraband,%", JournalSql.tagPattern("contraband"));
        assertEquals("%,black\\_market\\%,%", JournalSql.tagPattern("black_market%"));
    }
}
