package org.skysongdev.skysongEconomy.journal;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.journal.RecordKind;
import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryRequestTest {
    private static final UUID ANNA = UUID.nameUUIDFromBytes("anna".getBytes());

    private static HistoryRequest parse(String... args) {
        return HistoryRequest.parse(args, name -> name.equalsIgnoreCase("Anna") ? Optional.of(ANNA) : Optional.empty());
    }

    private static String refusal(String... args) {
        return assertThrows(RefusedException.class, () -> parse(args)).messageKey();
    }

    @Test
    void withNothingItShowsTheNewestTenRecords() {
        HistoryRequest request = parse();

        assertEquals("all", request.filter());
        assertEquals(1, request.page());
        assertTrue(request.query().newestFirst());
        assertEquals(HistoryRequest.PAGE_SIZE, request.query().limit());
        assertEquals(0, request.query().offset());
    }

    @Test
    void aPageSkipsTheNewerPages() {
        assertEquals(20, parse("3").query().offset());
        assertEquals(10, parse("business", "Tavern", "2").query().offset());
    }

    @Test
    void readsEachFilter() {
        assertEquals(Optional.of("tavern"), parse("business", "Tavern").query().businessId());
        assertEquals("business Tavern", parse("business", "Tavern").filter());
        assertEquals(Optional.of("harbour"), parse("pier", "harbour").query().pierId());
        assertEquals(Optional.of(ANNA), parse("player", "anna").query().player());
        assertEquals(Set.of(RecordKind.IMPORT_ORDERED), parse("kind", "import-ordered").query().kinds());
        assertEquals(Set.of(RecordKind.CLAIM_BOUGHT), parse("KIND", "claim_bought").query().kinds());
    }

    @Test
    void refusesWhatItCantRead() {
        assertEquals("command.usage", refusal("business"));
        assertEquals("command.usage", refusal("2", "3"));
        assertEquals("command.unknown-player", refusal("player", "Nobody"));
        assertEquals("history.unknown-kind", refusal("kind", "theft"));
        assertEquals("history.invalid-page", refusal("0"));
        assertEquals("history.invalid-page", refusal("two"));
        assertEquals("history.invalid-page", refusal(String.valueOf(HistoryRequest.MAX_PAGE + 1)));
    }

    @Test
    void anUnknownKindListsTheKinds() {
        RefusedException refused = assertThrows(RefusedException.class, () -> parse("kind", "theft"));
        assertTrue(refused.placeholders().get("kinds").contains("import-ordered"));
    }
}
