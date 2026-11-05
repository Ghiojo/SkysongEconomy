package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.prices.PriceContext;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/** One line of an import bundle's {@code contents}. */
public sealed interface BundlePart {
    /** @return what this part gives for one delivery */
    List<ItemAmount> roll(RandomGenerator random);

    /** @return the average base value of what this part gives */
    BigDecimal averageValue(PriceContext items);

    /** @return the material ids this part can give */
    Set<String> materialIds();

    /**
     * An item with a fixed or random amount: {@code {item: steel_sword, amount: 2}} or
     * {@code {item: steel_spear, amount: "1-3"}}.
     */
    record Item(String materialId, IntRange amount) implements BundlePart {
        public Item {
            Objects.requireNonNull(materialId, "materialId");
            Objects.requireNonNull(amount, "amount");
        }

        @Override
        public List<ItemAmount> roll(RandomGenerator random) {
            int rolled = amount.roll(random);
            return rolled == 0 ? List.of() : List.of(new ItemAmount(materialId, rolled));
        }

        @Override
        public BigDecimal averageValue(PriceContext items) {
            return items.item(materialId).multiply(amount.average());
        }

        @Override
        public Set<String> materialIds() {
            return Set.of(materialId);
        }
    }

    /**
     * A number of rolls, each picking one option by weight:
     * {@code {rolls: "1-2", from: [{item: iron_dagger, weight: 3, amount: 1}, ...]}}.
     */
    record Rolls(IntRange rolls, List<Option> options) implements BundlePart {
        public Rolls {
            Objects.requireNonNull(rolls, "rolls");
            options = List.copyOf(options);
            if (options.isEmpty()) {
                throw new IllegalArgumentException("Rolls need at least one option");
            }
        }

        /**
         * @param weight how likely it is compared to the others, above 0
         */
        public record Option(String materialId, int weight, IntRange amount) {
            public Option {
                Objects.requireNonNull(materialId, "materialId");
                Objects.requireNonNull(amount, "amount");
                if (weight < 1) {
                    throw new IllegalArgumentException("weight must be 1 or more: " + weight);
                }
            }
        }

        /** @return the chance in percent that one roll picks that option, rounded to a whole number */
        public int chance(Option option) {
            return Math.round(option.weight() * 100f / totalWeight());
        }

        private int totalWeight() {
            return options.stream().mapToInt(Option::weight).sum();
        }

        @Override
        public List<ItemAmount> roll(RandomGenerator random) {
            List<ItemAmount> result = new ArrayList<>();
            int count = rolls.roll(random);
            for (int i = 0; i < count; i++) {
                int ticket = random.nextInt(totalWeight());
                for (Option option : options) {
                    ticket -= option.weight();
                    if (ticket < 0) {
                        int amount = option.amount().roll(random);
                        if (amount > 0) {
                            result.add(new ItemAmount(option.materialId(), amount));
                        }
                        break;
                    }
                }
            }
            return result;
        }

        @Override
        public BigDecimal averageValue(PriceContext items) {
            BigDecimal perRoll = BigDecimal.ZERO;
            for (Option option : options) {
                perRoll = perRoll.add(items.item(option.materialId())
                        .multiply(option.amount().average())
                        .multiply(BigDecimal.valueOf(option.weight())));
            }
            return perRoll.multiply(rolls.average()).divide(BigDecimal.valueOf(totalWeight()), MathContext.DECIMAL64);
        }

        @Override
        public Set<String> materialIds() {
            return options.stream().map(Option::materialId).collect(Collectors.toUnmodifiableSet());
        }
    }
}
