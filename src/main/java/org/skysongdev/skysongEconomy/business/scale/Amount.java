package org.skysongdev.skysongEconomy.business.scale;

import org.skysongdev.skysongEconomy.expression.Formula;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A whole number that may follow a business's level and tier: how many NPC slots a skill gives,
 * how many stat points an NPC gets, how many modules a business may pick.
 * <p>
 * Config files write one as a fixed number ({@code 4}), a formula
 * ({@code "3 + {level}"}) or a table ({@code {by: tier, 1: 1, 3: 2, 5: 3}}); {@link AmountParser}
 * reads all three. An amount is never negative: a formula or table that goes below 0 gives 0.
 */
public sealed interface Amount permits Amount.Fixed, Amount.Computed, Amount.Table {
    /** An amount that is always 0. */
    Amount NONE = new Fixed(0);

    /**
     * @param scale how far the business has come
     * @return the amount there, 0 or more
     */
    int at(BusinessScale scale);

    /**
     * The same number whatever the business's level and tier.
     *
     * @param value the number, 0 or more
     */
    record Fixed(int value) implements Amount {
        /**
         * @throws IllegalArgumentException when the value is negative
         */
        public Fixed {
            if (value < 0) {
                throw new IllegalArgumentException("An amount is 0 or more, not " + value);
            }
        }

        @Override
        public int at(BusinessScale scale) {
            return value;
        }
    }

    /**
     * A formula over {@code {level}} and {@code {tier}}. Its result is truncated to a whole
     * number, and a result below 0 gives 0.
     *
     * @param formula the formula; {@link AmountParser} makes sure it only uses those two
     *                placeholders and never divides by zero
     */
    record Computed(Formula formula) implements Amount {
        public Computed {
            Objects.requireNonNull(formula, "formula");
        }

        @Override
        public int at(BusinessScale scale) {
            BigDecimal exact = formula.evaluate(placeholder ->
                    BigDecimal.valueOf(scale.value(Basis.fromKey(placeholder).orElseThrow())));
            return whole(exact);
        }
    }

    /**
     * A table by level or by tier. Below its first row it gives 0.
     *
     * @param steps the rows, each 0 or more
     */
    record Table(Steps<Integer> steps) implements Amount {
        /**
         * @throws IllegalArgumentException when a row is negative
         */
        public Table {
            Objects.requireNonNull(steps, "steps");
            steps.rows().forEach((row, value) -> {
                if (value < 0) {
                    throw new IllegalArgumentException("An amount is 0 or more, not " + value + " in row " + row);
                }
            });
        }

        @Override
        public int at(BusinessScale scale) {
            return steps.at(scale).orElse(0);
        }
    }

    /**
     * @param exact any number
     * @return it without its decimals, held between 0 and the largest whole number an amount can be
     */
    static int whole(BigDecimal exact) {
        BigDecimal truncated = exact.setScale(0, RoundingMode.DOWN);
        if (truncated.signum() <= 0) {
            return 0;
        }
        return truncated.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) >= 0 ? Integer.MAX_VALUE : truncated.intValue();
    }
}
