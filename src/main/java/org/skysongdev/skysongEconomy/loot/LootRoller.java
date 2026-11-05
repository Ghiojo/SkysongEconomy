package org.skysongdev.skysongEconomy.loot;

import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/** Rolls a {@link LootOutput} into the items of one harvest. */
public final class LootRoller {
    private final RandomGenerator random;

    public LootRoller(RandomGenerator random) {
        this.random = random;
    }

    /** Same as {@link #roll(LootOutput, YieldAdjuster)} with no adjustments. */
    public List<ItemAmount> roll(LootOutput output) {
        return roll(output, YieldAdjuster.NONE);
    }

    /**
     * @return the items, one entry per material id with amounts added up, in the order they were
     *         first rolled; items that rolled 0 are left out
     */
    public List<ItemAmount> roll(LootOutput output, YieldAdjuster adjuster) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (LootEntry.Item item : output.guaranteed()) {
            give(item, adjuster, totals);
        }
        for (ChanceItem chance : output.chances()) {
            if (random.nextDouble() * 100 < chance.percent()) {
                give(chance.item(), adjuster, totals);
            }
        }
        for (GroupRolls rolls : output.rolls()) {
            int count = Math.max(0, adjuster.rolls(rolls.count().roll(random)));
            for (int i = 0; i < count; i++) {
                rollOnce(rolls.group(), adjuster, totals);
            }
        }
        List<ItemAmount> result = new ArrayList<>(totals.size());
        totals.forEach((id, amount) -> result.add(new ItemAmount(id, amount)));
        return result;
    }

    private void rollOnce(LootGroup group, YieldAdjuster adjuster, Map<String, Integer> totals) {
        LootEntry entry = group.pick(random);
        // Nested groups are checked for loops when they are built, so this always ends.
        while (entry instanceof LootEntry.Group nested) {
            entry = nested.group().pick(random);
        }
        if (entry instanceof LootEntry.Item item) {
            give(item, adjuster, totals);
        }
    }

    private void give(LootEntry.Item item, YieldAdjuster adjuster, Map<String, Integer> totals) {
        int amount = adjuster.amount(item.amount()).roll(random);
        if (amount > 0) {
            totals.merge(item.materialId(), amount, Integer::sum);
        }
    }
}
