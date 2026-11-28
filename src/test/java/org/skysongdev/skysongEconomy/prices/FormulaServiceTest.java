package org.skysongdev.skysongEconomy.prices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.items.InvalidPriceFormulaException;
import org.skysongdev.skysongEconomy.api.items.PriceFormula;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaServiceTest {
    private final Map<String, String> saved = new HashMap<>();
    private PriceDatabase prices;
    private FormulaService formulas;

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
        formulas = new FormulaService(prices, new PriceSettings(1));
    }

    @Test
    void aPlainNumberIsItsOwnValue() {
        PriceFormula formula = formulas.parse(" 12.5 ");

        assertEquals("12.5", formula.text());
        assertEquals(Optional.of(new BigDecimal("12.5")), formula.value());
        assertFalse(formula.usesBase());
        assertTrue(formula.items().isEmpty());
    }

    @Test
    void itemReferencesReadThePriceDatabaseAndAreTruncated() {
        prices.set("steel_ingot", "4");

        PriceFormula formula = formulas.parse("{item:steel_ingot} * 1.23");

        assertEquals(Optional.of(new BigDecimal("4.9")), formula.value());
        assertEquals(List.of("steel_ingot"), List.copyOf(formula.items()));
    }

    @Test
    void valuesFollowThePriceDatabaseAfterParsing() {
        prices.set("grain", "2");
        PriceFormula formula = formulas.parse("{item:grain} * 3");

        prices.set("grain", "5");

        assertEquals(Optional.of(new BigDecimal("15.0")), formula.value());
    }

    @Test
    void baseIsTheValueTheCallerGives() {
        prices.set("spice", "10");
        PriceFormula formula = formulas.parse("{base} * 0.8 + {item:spice}");

        assertTrue(formula.usesBase());
        assertEquals(Optional.of(new BigDecimal("13.2")), formula.value(new BigDecimal("4")));
    }

    @Test
    void baseWithoutAValueIsEmpty() {
        assertEquals(Optional.empty(), formulas.parse("{base} * 2").value());
    }

    @Test
    void anItemWithoutABaseValueIsEmpty() {
        PriceFormula formula = formulas.parse("{item:unknown_thing} + 1");

        assertEquals(Optional.empty(), formula.value());
        assertEquals(Optional.empty(), formula.value(BigDecimal.ONE));
    }

    @Test
    void dividingByZeroIsEmpty() {
        assertEquals(Optional.empty(), formulas.parse("4 / ({base} - 2)").value(new BigDecimal("2")));
    }

    @Test
    void aNegativeResultBecomesZero() {
        assertEquals(Optional.of(new BigDecimal("0.0")), formulas.parse("{base} - 10").value(BigDecimal.ONE));
    }

    @Test
    void itemIdsAreGivenInLowerCase() {
        assertEquals(List.of("steel_ingot"), List.copyOf(formulas.parse("{item:Steel_Ingot}").items()));
    }

    @Test
    void anInvalidFormulaSaysWhatIsWrong() {
        InvalidPriceFormulaException e = assertThrows(InvalidPriceFormulaException.class,
                () -> formulas.parse("{cost} * 2"));

        assertEquals("{cost} * 2", e.formula());
        assertTrue(e.problem().contains("{cost}"), e.problem());
    }

    @Test
    void unbalancedParenthesesAreRefused() {
        assertThrows(InvalidPriceFormulaException.class, () -> formulas.parse("(1 + 2"));
    }

    @Test
    void formulasWithTheSameTextAreEqual() {
        assertEquals(formulas.parse("{base} * 2"), formulas.parse("{base} * 2"));
    }
}
