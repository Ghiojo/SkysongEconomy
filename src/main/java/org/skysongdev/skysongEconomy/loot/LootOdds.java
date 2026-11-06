package org.skysongdev.skysongEconomy.loot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Works out the exact odds of an output, for balancing: for each item, the chance of getting at
 * least one in a harvest and how many come on average. Boosters aren't counted.
 * <p>
 * Guaranteed items always come. Each chance item comes on its own. On a roll line, an item's
 * chance per roll is its weight's share of the group, multiplied through every nested group it
 * sits in, and the roll count is averaged over its range.
 */
public final class LootOdds {
    private LootOdds() {
    }

    /** @return every item the output can give, most likely first */
    public static List<ItemOdds> of(LootOutput output) {
        Map<String, Tally> tallies = new LinkedHashMap<>();
        for (LootEntry.Item item : output.guaranteed()) {
            tallies.computeIfAbsent(item.materialId(), id -> new Tally()).add(0, average(item));
        }
        for (ChanceItem chance : output.chances()) {
            double probability = chance.percent() / 100;
            tallies.computeIfAbsent(chance.item().materialId(), id -> new Tally())
                    .add(1 - probability, probability * average(chance.item()));
        }
        for (GroupRolls rolls : output.rolls()) {
            Map<String, PerRoll> perRoll = new LinkedHashMap<>();
            flatten(rolls.group(), 1, perRoll);
            int min = rolls.count().min();
            int max = rolls.count().max();
            double averageTimes = (min + max) / 2.0;
            perRoll.forEach((id, odds) -> tallies.computeIfAbsent(id, key -> new Tally())
                    .add(averageMiss(1 - odds.chance, min, max), averageTimes * odds.amount));
        }
        List<ItemOdds> result = new ArrayList<>(tallies.size());
        tallies.forEach((id, tally) -> result.add(new ItemOdds(id, 1 - tally.miss, tally.amount)));
        result.sort(Comparator.comparingDouble(ItemOdds::chance).reversed().thenComparing(ItemOdds::materialId));
        return result;
    }

    /**
     * Works out what one harvest is worth on average.
     *
     * @param odds   from {@link #of}
     * @param values each item's value, empty for an item without one
     */
    public static AverageValue averageValue(List<ItemOdds> odds, Function<String, Optional<BigDecimal>> values) {
        BigDecimal total = BigDecimal.ZERO;
        List<String> unpriced = new ArrayList<>();
        for (ItemOdds item : odds) {
            Optional<BigDecimal> value = values.apply(item.materialId());
            if (value.isPresent()) {
                total = total.add(value.get().multiply(BigDecimal.valueOf(item.averageAmount())));
            } else {
                unpriced.add(item.materialId());
            }
        }
        return new AverageValue(total, unpriced);
    }

    /**
     * What one harvest is worth on average.
     *
     * @param value    the sum of each item's value times its average amount
     * @param unpriced the items left out because they have no value
     */
    public record AverageValue(BigDecimal value, List<String> unpriced) {
        public AverageValue {
            unpriced = List.copyOf(unpriced);
        }
    }

    /** Adds every item one roll on {@code group} can land on, each scaled by {@code share}. */
    private static void flatten(LootGroup group, double share, Map<String, PerRoll> into) {
        double total = group.totalWeight();
        for (WeightedEntry weighted : group.entries()) {
            double chance = share * weighted.weight() / total;
            switch (weighted.entry()) {
                case LootEntry.Item item -> {
                    PerRoll odds = into.computeIfAbsent(item.materialId(), id -> new PerRoll());
                    odds.chance += chance;
                    odds.amount += chance * average(item);
                }
                case LootEntry.Group nested -> flatten(nested.group(), chance, into);
                case LootEntry.Nothing ignored -> {
                }
            }
        }
    }

    /** @return the chance of missing on every roll, averaged over each roll count from min to max */
    private static double averageMiss(double missPerRoll, int min, int max) {
        double sum = 0;
        for (int times = min; times <= max; times++) {
            sum += Math.pow(missPerRoll, times);
        }
        return sum / (max - min + 1);
    }

    private static double average(LootEntry.Item item) {
        return (item.amount().min() + item.amount().max()) / 2.0;
    }

    /** One item's odds on a single roll. */
    private static final class PerRoll {
        private double chance;
        private double amount;
    }

    /** One item's odds over the whole harvest, from sources that are rolled independently. */
    private static final class Tally {
        private double miss = 1;
        private double amount;

        void add(double missChance, double averageAmount) {
            miss *= missChance;
            amount += averageAmount;
        }
    }
}
