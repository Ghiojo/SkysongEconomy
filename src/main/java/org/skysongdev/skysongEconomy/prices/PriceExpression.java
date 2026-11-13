package org.skysongdev.skysongEconomy.prices;

import org.skysongdev.skysongEconomy.expression.Formula;
import org.skysongdev.skysongEconomy.expression.FormulaException;
import org.skysongdev.skysongEconomy.expression.Placeholders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A price written as arithmetic, for example {@code {base} * 1.3} or
 * {@code {item:steel_ingot} * 2 + 1}.
 * <p>
 * It allows numbers, {@code + - * /}, parentheses, unary minus, {@code {base}} (the base value
 * of what is being priced) and {@code {item:<material_id>}} (another item's base value).
 * Expressions are parsed once and evaluated many times. The result is exact: truncate it with
 * {@link PriceSettings#truncate} where players see or pay it.
 * <p>
 * The arithmetic itself is a {@link Formula}; this class adds what the placeholders mean for
 * prices and reports problems as {@link PriceException}s.
 */
public final class PriceExpression {
    private static final String BASE = "base";
    private static final String ITEM_PREFIX = "item:";

    private final Formula formula;
    private final Set<String> items;
    private final boolean usesBase;

    private PriceExpression(Formula formula) {
        this.formula = formula;
        Set<String> referenced = new LinkedHashSet<>();
        for (String placeholder : formula.placeholders()) {
            if (placeholder.startsWith(ITEM_PREFIX)) {
                referenced.add(placeholder.substring(ITEM_PREFIX.length()));
            }
        }
        this.items = Collections.unmodifiableSet(referenced);
        this.usesBase = formula.placeholders().contains(BASE);
    }

    /**
     * @throws PriceException {@code prices.invalid-expression} when the text isn't a valid expression
     */
    public static PriceExpression parse(String text) {
        Objects.requireNonNull(text, "text");
        try {
            return new PriceExpression(Formula.parse(text, PriceExpression::placeholder));
        } catch (FormulaException e) {
            throw new PriceException("prices.invalid-expression", Map.of("expression", e.formula(), "problem", e.problem()));
        }
    }

    /** @return an expression that is just this number */
    public static PriceExpression of(BigDecimal value) {
        return parse(value.toPlainString());
    }

    /**
     * @throws PriceException when a value it needs is missing, or it divides by zero
     */
    public BigDecimal evaluate(PriceContext context) {
        try {
            return formula.evaluate(placeholder -> placeholder.equals(BASE)
                    ? context.base()
                    : context.item(placeholder.substring(ITEM_PREFIX.length())));
        } catch (FormulaException e) {
            throw new PriceException("prices.division-by-zero", Map.of());
        }
    }

    /** @return the material ids its {@code {item:...}} references name, in order */
    public Set<String> items() {
        return items;
    }

    /** @return whether it uses {@code {base}} */
    public boolean usesBase() {
        return usesBase;
    }

    /** @return the expression as written, without surrounding spaces */
    public String text() {
        return formula.text();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PriceExpression expression && expression.formula.equals(formula);
    }

    @Override
    public int hashCode() {
        return formula.hashCode();
    }

    @Override
    public String toString() {
        return formula.text();
    }

    /** The placeholders a price may use: {@code {base}} and {@code {item:<material_id>}}. */
    private static String placeholder(String written) {
        if (written.equals(BASE)) {
            return BASE;
        }
        if (written.startsWith(ITEM_PREFIX)) {
            String materialId = written.substring(ITEM_PREFIX.length()).strip();
            if (materialId.isEmpty()) {
                throw new Placeholders.Unknown("has {item:} without a material id");
            }
            return ITEM_PREFIX + materialId;
        }
        throw new Placeholders.Unknown("has {" + written + "}: use {base} or {item:<material_id>}");
    }
}
