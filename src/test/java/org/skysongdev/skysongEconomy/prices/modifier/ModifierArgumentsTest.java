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
        ModifierDraft draft = parse("Port_Tax ledger:Smugglers +10%");
        assertEquals("port_tax", draft.type());
        assertEquals(new ModifierTarget.Ledger("smugglers"), draft.target());
        assertEquals(new ModifierAmount.Percent(BigDecimal.TEN), draft.amount());
        assertEquals(AppliesTo.IMPORTS, draft.appliesTo());
        assertNull(draft.expiresAt());
        assertNull(draft.displayName());
    }

    @Test
    void readsOptionsAndTheDisplayName() {
        ModifierDraft draft = parse("festival global -5% for:1d12h on:both <gold>Harvest festival");
        assertEquals(new ModifierAmount.Percent(new BigDecimal("-5")), draft.amount());
        assertEquals(NOW.plus(Duration.ofHours(36)), draft.expiresAt());
        assertEquals(AppliesTo.BOTH, draft.appliesTo());
        assertEquals("<gold>Harvest festival", draft.displayName());
    }

    @Test
    void aPlainNumberIsFlatAndClaimTargetsDefaultToClaims() {
        ModifierDraft fee = parse("registry_fee claim-type:House +50 Registry fee");
        assertEquals(new ModifierAmount.Flat(new BigDecimal("50")), fee.amount());
        assertEquals(new ModifierTarget.ClaimType("house"), fee.target());
        assertEquals(AppliesTo.CLAIMS, fee.appliesTo());
        assertEquals("Registry fee", fee.displayName());

        ModifierDraft festival = parse("festival global +15% on:claims");
        assertEquals(AppliesTo.CLAIMS, festival.appliesTo());
        assertEquals(new ModifierTarget.Claim("rose_house"), parse("damage claim:rose_house +5%").target());
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
        assertKey("prices.modifiers.invalid-amount", "tax global ten");
        assertKey("prices.modifiers.invalid-amount", "tax global ten%");
        assertKey("prices.modifiers.invalid-duration", "tax global 10 for:soon");
        assertKey("prices.modifiers.invalid-duration", "tax global 10 for:0h");
        assertKey("prices.modifiers.invalid-duration", "tax global 10 for:5h3");
        assertKey("prices.modifiers.invalid-applies-to", "tax global 10 on:everything");
    }

    private static void assertKey(String key, String line) {
        assertEquals(key, assertThrows(PriceException.class, () -> parse(line), line).messageKey());
    }
}
