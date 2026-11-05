package org.skysongdev.skysongEconomy.prices.modifier;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.prices.PriceException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModifierArgumentsTest {
    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

    private static ModifierDraft parse(String line) {
        return ModifierArguments.parse(line.split(" "), "Staff", NOW);
    }

    @Test
    void readsTheRequiredWordsWithDefaults() {
        ModifierDraft draft = parse("Port_Tax ledger:Smugglers +10");
        assertEquals("port_tax", draft.type());
        assertEquals(new ModifierTarget.Ledger("smugglers"), draft.target());
        assertEquals(0, BigDecimal.TEN.compareTo(draft.percent()));
        assertEquals(AppliesTo.IMPORTS, draft.appliesTo());
        assertNull(draft.expiresAt());
        assertNull(draft.displayName());
    }

    @Test
    void readsOptionsAndTheDisplayName() {
        ModifierDraft draft = parse("festival global -5% for:1d12h on:both <gold>Harvest festival");
        assertEquals(0, new BigDecimal("-5").compareTo(draft.percent()));
        assertEquals(NOW.plus(Duration.ofHours(36)), draft.expiresAt());
        assertEquals(AppliesTo.BOTH, draft.appliesTo());
        assertEquals("<gold>Harvest festival", draft.displayName());
    }

    @Test
    void readsDurations() {
        assertEquals(Duration.ofMinutes(30), ModifierArguments.duration("30m"));
        assertEquals(Duration.ofDays(7), ModifierArguments.duration("7D"));
        assertEquals(Duration.ofHours(26).plusMinutes(5), ModifierArguments.duration("1d2h5m"));
    }

    @Test
    void refusesBrokenWords() {
        assertKey("prices.modifiers.invalid-target", "tax shop:x 10");
        assertKey("prices.modifiers.invalid-target", "tax item: 10");
        assertKey("prices.modifiers.invalid-percent", "tax global ten");
        assertKey("prices.modifiers.invalid-duration", "tax global 10 for:soon");
        assertKey("prices.modifiers.invalid-duration", "tax global 10 for:0h");
        assertKey("prices.modifiers.invalid-duration", "tax global 10 for:5h3");
        assertKey("prices.modifiers.invalid-applies-to", "tax global 10 on:everything");
    }

    private static void assertKey(String key, String line) {
        assertEquals(key, assertThrows(PriceException.class, () -> parse(line), line).messageKey());
    }
}
