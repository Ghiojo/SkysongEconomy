package org.skysongdev.skysongEconomy.prices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.trade.GoodTypeIndex;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemLookupTest {
    private final Map<String, String> saved = new HashMap<>();
    private final Map<String, Set<String>> tagged = new HashMap<>();
    private PriceDatabase prices;
    private ItemLookup catalog;

    @BeforeEach
    void setUp() {
        prices = new PriceDatabase(new BaseValueRepository() {
            @Override
            public Map<String, String> loadAll() {
                return new HashMap<>(saved);
            }

            @Override
            public void save(String materialId, String expression, BigDecimal valueNow) {
                saved.put(materialId, expression);
            }

            @Override
            public void delete(String materialId) {
                saved.remove(materialId);
            }
        });
        GoodTypeIndex goodTypes = new GoodTypeIndex() {
            @Override
            public Map<String, Set<String>> all() {
                return tagged;
            }

            @Override
            public Set<String> of(String materialId) {
                return tagged.getOrDefault(materialId, Set.of());
            }
        };
        catalog = new ItemLookup(prices, new PriceSettings(1), goodTypes);
    }

    private void tag(String materialId, String... goodTypes) {
        tagged.computeIfAbsent(materialId, id -> new LinkedHashSet<>()).addAll(List.of(goodTypes));
    }

    @Test
    void baseValuesAreTruncatedToTheConfiguredDecimals() {
        prices.set("steel_ingot", "4.09");
        prices.set("steel_sword", "{item:steel_ingot} * 2 + 1");

        assertEquals(new BigDecimal("4.0"), catalog.baseValue("steel_ingot").orElseThrow());
        assertEquals(new BigDecimal("9.1"), catalog.baseValue("steel_sword").orElseThrow());
        assertEquals(1, catalog.decimals());
    }

    @Test
    void baseValuesFollowChangesAtOnce() {
        prices.set("steel_ingot", "4");
        prices.set("steel_sword", "{item:steel_ingot} * 2");
        prices.set("steel_ingot", "5");

        assertEquals(new BigDecimal("10.0"), catalog.baseValue("steel_sword").orElseThrow());
    }

    @Test
    void materialIdsIgnoreCaseAndSpaces() {
        prices.set("steel_ingot", "4");

        assertEquals(new BigDecimal("4.0"), catalog.baseValue("  Steel_Ingot ").orElseThrow());
    }

    @Test
    void anItemWithoutAValueHasNone() {
        assertTrue(catalog.baseValue("unknown").isEmpty());
    }

    @Test
    void aValueThatCantBeWorkedOutIsEmptyAndLeftOutOfTheList() {
        saved.put("broken", "1 / 0");
        saved.put("steel_ingot", "4");
        prices.loadAll(warning -> { });

        assertTrue(catalog.baseValue("broken").isEmpty());
        assertEquals(Map.of("steel_ingot", new BigDecimal("4.0")), catalog.baseValues());
    }

    @Test
    void everyBaseValueIsListedAlphabeticallyAndCantBeChanged() {
        prices.set("wheat", "1");
        prices.set("bread", "{item:wheat} * 3");

        assertEquals(List.of("bread", "wheat"), List.copyOf(catalog.baseValues().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> catalog.baseValues().put("x", BigDecimal.ONE));
    }

    @Test
    void goodTypesOfAnItemIgnoreCaseAndAreSorted() {
        tag("steel_sword", "weapon", "metalwork");

        assertEquals(List.of("metalwork", "weapon"), List.copyOf(catalog.goodTypes("STEEL_SWORD")));
        assertTrue(catalog.goodTypes("bread").isEmpty());
    }

    @Test
    void hasGoodTypeIgnoresCase() {
        tag("steel_sword", "Weapon");

        assertTrue(catalog.hasGoodType("steel_sword", "weapon"));
        assertTrue(catalog.hasGoodType("Steel_Sword", " WEAPON "));
        assertFalse(catalog.hasGoodType("steel_sword", "food"));
        assertFalse(catalog.hasGoodType("bread", "weapon"));
    }

    @Test
    void everyGoodTypeIsListedOnce() {
        tag("steel_sword", "weapon", "metalwork");
        tag("iron_axe", "weapon", "tool");

        assertEquals(List.of("metalwork", "tool", "weapon"), List.copyOf(catalog.allGoodTypes()));
    }

    @Test
    void itemsOfAGoodTypeAreListedInLowerCase() {
        tag("Steel_Sword", "weapon");
        tag("iron_axe", "WEAPON", "tool");
        tag("bread", "food");

        assertEquals(List.of("iron_axe", "steel_sword"), List.copyOf(catalog.itemsOf("weapon")));
        assertTrue(catalog.itemsOf("contraband").isEmpty());
    }

    @Test
    void returnedGoodTypesAreCopies() {
        tag("steel_sword", "weapon");
        Set<String> types = catalog.goodTypes("steel_sword");
        tag("steel_sword", "metalwork");

        assertEquals(Set.of("weapon"), types);
        assertThrows(UnsupportedOperationException.class, () -> types.add("food"));
    }
}
