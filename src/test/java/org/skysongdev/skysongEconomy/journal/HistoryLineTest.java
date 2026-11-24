package org.skysongdev.skysongEconomy.journal;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.journal.EconomyRecord;
import org.skysongdev.skysongEconomy.api.journal.JournalIcDate;
import org.skysongdev.skysongEconomy.api.journal.MoneyAccount;
import org.skysongdev.skysongEconomy.api.journal.MoneyMovement;
import org.skysongdev.skysongEconomy.api.journal.RecordItem;
import org.skysongdev.skysongEconomy.api.journal.RecordKind;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoryLineTest {
    private static final UUID BRAM = UUID.nameUUIDFromBytes("bram".getBytes());

    @Test
    void showsTimesWhoBusinessItemsAndMoney() {
        EconomyRecord record = new EconomyRecord(5, UUID.randomUUID(), RecordKind.IMPORT_ORDERED, "s1",
                Instant.parse("2026-11-24T12:05:00Z"), Optional.of(JournalIcDate.of(1203, "Idalia", 3, 47, false)),
                Optional.of(new CharacterRef(BRAM, "Bram the Bold")), Optional.of("tavern"), Optional.of("<gold>Tavern"),
                Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(new MoneyMovement(MoneyAccount.player(BRAM), MoneyAccount.SERVER, new BigDecimal("120")),
                        new MoneyMovement(MoneyAccount.SERVER, MoneyAccount.vault("tavern"), new BigDecimal("4"))),
                List.of(new RecordItem("red_wine", 4, RecordItem.Flow.IN), new RecordItem("cork", 1, RecordItem.Flow.LOST)),
                Map.of());

        HistoryLine line = HistoryLine.of(record, player -> player.equals(BRAM) ? "bram_mc" : "?",
                amount -> amount.toPlainString() + "a", ZoneOffset.UTC);

        assertEquals(new HistoryLine("2026-11-24 12:05", "Idalia 3, 1203", "import-ordered", "Bram the Bold",
                "<gold>Tavern", "4 red_wine in, 1 cork lost", "120a bram_mc → server, 4a server → vault of tavern"), line);
    }

    @Test
    void showsADashForNobodyAndNothing() {
        EconomyRecord record = new EconomyRecord(6, UUID.randomUUID(), RecordKind.CLAIM_PAYOUT, "claim-sale:1",
                Instant.parse("2026-11-24T12:05:00Z"), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("rose_house"), List.of(), List.of(),
                Map.of());

        HistoryLine line = HistoryLine.of(record, player -> "?", BigDecimal::toPlainString, ZoneOffset.UTC);

        assertEquals(new HistoryLine("2026-11-24 12:05", "", "claim-payout", "-", "", "-", "-"), line);
    }
}
