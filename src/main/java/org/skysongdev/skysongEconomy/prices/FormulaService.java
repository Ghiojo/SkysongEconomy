package org.skysongdev.skysongEconomy.prices;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.items.InvalidPriceFormulaException;
import org.skysongdev.skysongEconomy.api.items.PriceFormula;
import org.skysongdev.skysongEconomy.api.items.PriceFormulas;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The {@link PriceFormulas} other plugins use: {@link PriceExpression} for the parsing, the
 * {@link PriceDatabase} for {@code {item:...}} values and {@link PriceSettings} for the decimals.
 * Kept free of Bukkit classes so it can be tested on its own.
 * <p>
 * Not thread-safe: use it on the main thread, like the classes it reads.
 */
public final class FormulaService implements PriceFormulas {
    private final PriceDatabase prices;
    private final PriceSettings settings;

    /**
     * @param prices   every item's base value
     * @param settings how many decimals prices keep
     */
    public FormulaService(PriceDatabase prices, PriceSettings settings) {
        this.prices = Objects.requireNonNull(prices, "prices");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public PriceFormula parse(String text) {
        Objects.requireNonNull(text, "text");
        try {
            return new Parsed(PriceExpression.parse(text));
        } catch (PriceException e) {
            throw new InvalidPriceFormulaException(text.strip(),
                    e.placeholders().getOrDefault("problem", "is not a valid formula"));
        }
    }

    /** One parsed formula, worked out against the price database as it is when asked. */
    private final class Parsed implements PriceFormula {
        private final PriceExpression expression;

        private Parsed(PriceExpression expression) {
            this.expression = expression;
        }

        @Override
        public String text() {
            return expression.text();
        }

        @Override
        public Set<String> items() {
            Set<String> ids = new LinkedHashSet<>();
            expression.items().forEach(id -> ids.add(id.toLowerCase(Locale.ROOT)));
            return Collections.unmodifiableSet(ids);
        }

        @Override
        public boolean usesBase() {
            return expression.usesBase();
        }

        @Override
        public Optional<BigDecimal> value() {
            return evaluate(null);
        }

        @Override
        public Optional<BigDecimal> value(BigDecimal base) {
            return evaluate(Objects.requireNonNull(base, "base"));
        }

        private Optional<BigDecimal> evaluate(@Nullable BigDecimal base) {
            try {
                BigDecimal value = expression.evaluate(prices.context(base));
                return Optional.of(settings.truncate(value.max(BigDecimal.ZERO)));
            } catch (PriceException e) {
                return Optional.empty();
            }
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Parsed parsed && parsed.expression.equals(expression);
        }

        @Override
        public int hashCode() {
            return expression.hashCode();
        }

        @Override
        public String toString() {
            return expression.text();
        }
    }
}
