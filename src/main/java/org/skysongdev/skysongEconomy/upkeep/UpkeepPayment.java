package org.skysongdev.skysongEconomy.upkeep;

import org.skysongdev.skysongEconomy.trade.GoodTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Works out what one cycle eats from a stock. Entries are paid in the order the profile lists
 * them, each from the first stacks that match it, and one item pays only one entry. It is all or
 * nothing: when any entry is short, nothing is taken.
 */
public final class UpkeepPayment {
    private final GoodTypes goodTypes;

    /** @param goodTypes every item's item categories, for {@code good-type:} entries */
    public UpkeepPayment(GoodTypes goodTypes) {
        this.goodTypes = Objects.requireNonNull(goodTypes, "goodTypes");
    }

    /** What paying one cycle would do. */
    public sealed interface Outcome {
        /** @return whether the cycle can be paid */
        boolean paid();
    }

    /**
     * The cycle can be paid.
     *
     * @param taken how many items to take from each stack, by the stack's place in the stock
     */
    public record Paid(List<Integer> taken) implements Outcome {
        public Paid {
            taken = List.copyOf(taken);
        }

        @Override
        public boolean paid() {
            return true;
        }
    }

    /**
     * The cycle can't be paid.
     *
     * @param missing each entry that is short, with how many items it still lacks
     */
    public record Short(List<Shortfall> missing) implements Outcome {
        public Short {
            missing = List.copyOf(missing);
        }

        @Override
        public boolean paid() {
            return false;
        }
    }

    /**
     * @param cost    the entry that is short
     * @param missing how many more items it needs
     */
    public record Shortfall(UpkeepCost cost, int missing) {
    }

    /**
     * @param cost  the cost of one cycle
     * @param stock the stacks in the NPC's stock, in their order
     * @return what to take, or what is missing
     */
    public Outcome plan(List<UpkeepCost> cost, List<StockStack> stock) {
        int[] left = new int[stock.size()];
        for (int i = 0; i < stock.size(); i++) {
            left[i] = stock.get(i).amount();
        }
        List<Shortfall> missing = new ArrayList<>();
        for (UpkeepCost entry : cost) {
            int needed = entry.amount();
            for (int i = 0; i < stock.size() && needed > 0; i++) {
                if (left[i] > 0 && matches(entry.match(), stock.get(i).materialId())) {
                    int take = Math.min(left[i], needed);
                    left[i] -= take;
                    needed -= take;
                }
            }
            if (needed > 0) {
                missing.add(new Shortfall(entry, needed));
            }
        }
        if (!missing.isEmpty()) {
            return new Short(missing);
        }
        List<Integer> taken = new ArrayList<>(stock.size());
        for (int i = 0; i < stock.size(); i++) {
            taken.add(stock.get(i).amount() - left[i]);
        }
        return new Paid(taken);
    }

    /**
     * @return how many whole cycles the stock pays for in a row, up to {@code max}
     */
    public int cyclesCovered(List<UpkeepCost> cost, List<StockStack> stock, int max) {
        List<StockStack> remaining = new ArrayList<>(stock);
        int cycles = 0;
        while (cycles < max && plan(cost, remaining) instanceof Paid paid) {
            List<StockStack> next = new ArrayList<>(remaining.size());
            for (int i = 0; i < remaining.size(); i++) {
                int amount = remaining.get(i).amount() - paid.taken().get(i);
                if (amount > 0) {
                    next.add(new StockStack(remaining.get(i).materialId(), amount));
                }
            }
            remaining = next;
            cycles++;
        }
        return cycles;
    }

    /** @return whether an item with that {@code material_id} pays at least one entry of the cost */
    public boolean accepts(List<UpkeepCost> cost, String materialId) {
        return cost.stream().anyMatch(entry -> matches(entry.match(), materialId));
    }

    private boolean matches(UpkeepCost.Match match, String materialId) {
        if (materialId == null) {
            return false;
        }
        return switch (match) {
            case UpkeepCost.Match.Item item -> item.materialId().equals(materialId);
            case UpkeepCost.Match.GoodType type -> goodTypes.of(materialId).contains(type.goodType());
        };
    }
}
