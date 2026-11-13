package org.skysongdev.skysongEconomy.business.scale;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmountParserTest {
    private static ConfigSection parse(String yaml) {
        return YamlFiles.parse("npcs.yml", new StringReader(yaml));
    }

    private static Amount amount(String yaml) {
        return AmountParser.require(parse(yaml), "amount");
    }

    private static BusinessScale scale(int level, int tier) {
        return new BusinessScale(level, tier);
    }

    private static String error(String yaml) {
        return assertThrows(ConfigException.class, () -> amount(yaml)).getMessage();
    }

    @Test
    void aNumberIsTheSameEverywhere() {
        Amount amount = amount("amount: 4");

        assertInstanceOf(Amount.Fixed.class, amount);
        assertEquals(4, amount.at(scale(1, 1)));
        assertEquals(4, amount.at(scale(5, 5)));
        assertEquals(2, amount("amount: 2.9").at(scale(1, 1)), "decimals are cut off");
    }

    @Test
    void aFormulaFollowsLevelAndTier() {
        Amount perLevel = amount("amount: \"3 + {level}\"");
        Amount perTier = amount("amount: \"2 + {tier}\"");

        assertEquals(4, perLevel.at(scale(1, 3)));
        assertEquals(6, perLevel.at(scale(3, 3)));
        assertEquals(5, perTier.at(scale(1, 3)));
        assertEquals(7, amount("amount: \"{ Level } * {TIER} + 1\"").at(scale(2, 3)));
    }

    @Test
    void aFormulaIsTruncatedAndNeverNegative() {
        assertEquals(1, amount("amount: \"{level} / 2\"").at(scale(3, 1)));
        assertEquals(0, amount("amount: \"{level} / 2\"").at(scale(1, 1)));
        assertEquals(0, amount("amount: \"{level} - 3\"").at(scale(1, 1)));
        assertEquals(2, amount("amount: \"{level} - 3\"").at(scale(5, 5)));
    }

    @Test
    void aTableAppliesEachRowFromItsNumberUpward() {
        Amount byTier = amount("amount: {by: tier, 1: 1, 3: 2, 5: 3}");

        assertInstanceOf(Amount.Table.class, byTier);
        assertEquals(1, byTier.at(scale(0, 1)));
        assertEquals(1, byTier.at(scale(0, 2)));
        assertEquals(2, byTier.at(scale(0, 3)));
        assertEquals(2, byTier.at(scale(0, 4)));
        assertEquals(3, byTier.at(scale(0, 5)));
    }

    @Test
    void aTableFollowsLevelUnlessItSaysTierAndGivesNothingBelowItsFirstRow() {
        Amount byLevel = amount("""
                amount:
                  2: 1
                  4: 3
                """);

        assertEquals(0, byLevel.at(scale(1, 5)));
        assertEquals(1, byLevel.at(scale(2, 5)));
        assertEquals(1, byLevel.at(scale(3, 5)));
        assertEquals(3, byLevel.at(scale(5, 5)));
    }

    @Test
    void aMissingKeyIsEmptyOrTheFallback() {
        ConfigSection section = parse("other: 1");

        assertTrue(AmountParser.read(section, "amount").isEmpty());
        assertSame(Amount.NONE, AmountParser.read(section, "amount", Amount.NONE));
        assertEquals(0, Amount.NONE.at(scale(5, 5)));
        assertEquals("npcs.yml: amount is required",
                assertThrows(ConfigException.class, () -> AmountParser.require(section, "amount")).getMessage());
    }

    @Test
    void explainsWhatIsWrongNamingTheKey() {
        assertEquals("npcs.yml: amount must be 0 or more", error("amount: -1"));
        assertEquals("npcs.yml: amount isn't a valid amount: '3 +' ends too early", error("amount: \"3 +\""));
        assertEquals("npcs.yml: amount isn't a valid amount: '{base} * 2' has {base}: use {level} or {tier}",
                error("amount: \"{base} * 2\""));
        assertEquals("npcs.yml: amount isn't a valid amount: '6 / ({level} - 2)' divides by zero at level 2, tier 1",
                error("amount: \"6 / ({level} - 2)\""));
        assertEquals("npcs.yml: amount must be a number, a formula like \"3 + {level}\", or a table like {by: tier, 1: 1, 3: 2}",
                error("amount: [1, 2]"));
        assertEquals("npcs.yml: amount.3 must be a number, 0 or more", error("amount: {1: 1, 3: -2}"));
        assertEquals("npcs.yml: amount.3 must be a number, 0 or more", error("amount: {1: 1, 3: two}"));
    }

    @Test
    void wholeCutsDecimalsAndStaysInRange() {
        assertEquals(0, Amount.whole(new java.math.BigDecimal("-0.5")));
        assertEquals(7, Amount.whole(new java.math.BigDecimal("7.99")));
        assertEquals(Integer.MAX_VALUE, Amount.whole(new java.math.BigDecimal("99999999999")));
    }

    @Test
    void amountsRefuseNegativeNumbers() {
        assertThrows(IllegalArgumentException.class, () -> new Amount.Fixed(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new Amount.Table(new Steps<>(Basis.LEVEL, java.util.Map.of(1, -1))));
    }
}
