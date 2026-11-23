package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.prices.modifier.TargetKind;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerParserTest {
    private static final Set<String> ITEMS = Set.of("steel_sword", "steel_spear", "iron_dagger", "steel_axe", "steel_ingot");

    private final List<String> warnings = new ArrayList<>();
    private final LedgerParser parser = new LedgerParser(Set.of("express", "regular", "slow"), ITEMS::contains, warnings::add);

    private Ledger parse(String yaml) {
        return parser.parse("test", YamlFiles.parse("ledgers/test.yml", new StringReader(yaml)));
    }

    @Test
    void bundledExampleLoadsWithoutWarnings() throws Exception {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/ledgers/smugglers.yml")), StandardCharsets.UTF_8)) {
            Ledger ledger = parser.parse("smugglers", YamlFiles.parse("ledgers/smugglers.yml", reader));
            assertEquals(List.of(), warnings);
            assertEquals("ENDER_CHEST", ledger.icon());
            assertEquals(Set.of(TargetKind.GLOBAL, TargetKind.GOOD_TYPE, TargetKind.ITEM), ledger.exemptFrom());
            ImportOffer weapons = ledger.importOffer("steel_weapons").orElseThrow();
            assertEquals(3, weapons.contents().parts().size());
            assertEquals(List.of("express", "regular", "slow"), List.copyOf(weapons.prices().keySet()));
            assertEquals(new ExportOffer.Target.Item("steel_ingot"), ledger.exportOffer("steel_ingots").orElseThrow().target());
            assertEquals(new ExportOffer.Target.GoodType("weapon"), ledger.exportOffer("any_weapon").orElseThrow().target());
            assertEquals(Set.of("contraband", "weapons"), ledger.shipmentTags(weapons));
        }
    }

    @Test
    void readsTagsOfTheLedgerAndItsImports() {
        Ledger ledger = parse("""
                name: Test
                tags: [" Contraband ", ""]
                imports:
                  crate:
                    contents: [{item: steel_sword}]
                    prices: {express: "10"}
                    tags: [weapons, contraband]
                  plain:
                    contents: [{item: steel_sword}]
                    prices: {express: "10"}
                """);

        assertEquals(Set.of("contraband"), ledger.tags());
        assertEquals(Set.of("contraband", "weapons"), ledger.shipmentTags(ledger.importOffer("crate").orElseThrow()));
        assertEquals(Set.of("contraband"), ledger.shipmentTags(ledger.importOffer("plain").orElseThrow()));
        assertEquals(Set.of(), parse("name: Untagged").tags());
    }

    @Test
    void readsEveryKindOfBundlePart() {
        Ledger ledger = parse("""
                name: Test
                imports:
                  crate:
                    contents:
                      - {item: steel_sword}
                      - {item: steel_spear, amount: "1-3"}
                      - rolls: 2
                        from:
                          - {item: iron_dagger, weight: 3}
                    prices: {express: "{base} * 2"}
                """);
        List<BundlePart> parts = ledger.importOffer("crate").orElseThrow().contents().parts();
        assertEquals(new BundlePart.Item("steel_sword", IntRange.exactly(1)), parts.get(0));
        assertEquals(new BundlePart.Item("steel_spear", new IntRange(1, 3)), parts.get(1));
        BundlePart.Rolls rolls = assertInstanceOf(BundlePart.Rolls.class, parts.get(2));
        assertEquals(IntRange.exactly(2), rolls.rolls());
        assertEquals(3, rolls.options().get(0).weight());
    }

    @Test
    void anImportMayLeaveOutDeliveryTimes() {
        Ledger ledger = parse("""
                name: Test
                imports:
                  crate:
                    contents: [{item: steel_sword}]
                    prices: {slow: "10"}
                """);
        ImportOffer crate = ledger.importOffer("crate").orElseThrow();
        assertTrue(crate.price("slow").isPresent());
        assertFalse(crate.price("express").isPresent());
    }

    @Test
    void unknownDeliveryTimesAreIgnoredWithAWarning() {
        Ledger ledger = parse("""
                name: Test
                imports:
                  crate:
                    contents: [{item: steel_sword}]
                    prices: {slow: "10", teleport: "99"}
                """);
        assertEquals(Set.of("slow"), ledger.importOffer("crate").orElseThrow().prices().keySet());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("teleport"), warnings.get(0));
    }

    @Test
    void brokenEntriesAreSkippedAndTheRestLoads() {
        Ledger ledger = parse("""
                name: Test
                imports:
                  unknown_item: {contents: [{item: mithril}], prices: {slow: "1"}}
                  no_prices: {contents: [{item: steel_sword}]}
                  bad_price: {contents: [{item: steel_sword}], prices: {slow: "{base} *"}}
                  empty: {contents: [], prices: {slow: "1"}}
                  bad_rolls: {contents: [{rolls: 1, from: []}], prices: {slow: "1"}}
                  good: {contents: [{item: steel_sword}], prices: {slow: "1"}}
                exports:
                  both: {item: steel_ingot, good-type: metal, price: "1"}
                  neither: {price: "1"}
                  zero_batch: {item: steel_ingot, batch: 0, price: "1"}
                  no_price: {item: steel_ingot}
                  good: {good-type: Metal, batch: 4, price: "{base}"}
                """);
        assertEquals(Set.of("good"), ledger.imports().keySet());
        assertEquals(Set.of("good"), ledger.exports().keySet());
        assertEquals(new ExportOffer.Target.GoodType("metal"), ledger.exportOffer("good").orElseThrow().target());
        assertEquals(9, warnings.size(), String.join("\n", warnings));
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("mithril")));
    }

    @Test
    void aLedgerNeedsAName() {
        assertThrows(ConfigException.class, () -> parse("icon: CHEST"));
    }

    @Test
    void aLedgerWithoutExemptFromPaysEveryModifier() {
        Ledger ledger = parse("name: Test");
        assertFalse(ledger.isTaxExempt());
        assertEquals(Set.of(), ledger.exemptFrom());
    }

    @Test
    void unknownExemptionsAndTheLedgerKindAreIgnoredWithAWarning() {
        Ledger ledger = parse("""
                name: Test
                exempt-from: [Global, ledger, taxes]
                """);
        assertEquals(Set.of(TargetKind.GLOBAL), ledger.exemptFrom());
        assertTrue(ledger.isTaxExempt());
        assertEquals(2, warnings.size());
        assertTrue(warnings.get(0).contains("ledger"), warnings.get(0));
        assertTrue(warnings.get(1).contains("taxes"), warnings.get(1));
    }
}
