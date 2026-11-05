package org.skysongdev.skysongEconomy.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Finds the items an export would take from an inventory: exactly one batch, or nothing.
 * Stacks are taken in inventory order.
 */
public final class ExportBatch {
    /** A stack of a Skysong item in an inventory slot. */
    public record Stack(int slot, String materialId, int amount) {
        public Stack {
            Objects.requireNonNull(materialId, "materialId");
        }
    }

    /** How many items to take from one slot. */
    public record Take(int slot, String materialId, int amount) {
        public Take {
            Objects.requireNonNull(materialId, "materialId");
        }
    }

    private ExportBatch() {
    }

    /** @return whether an item counts for that export */
    public static boolean matches(ExportOffer.Target target, String materialId, GoodTypes goodTypes) {
        return switch (target) {
            case ExportOffer.Target.Item item -> item.materialId().equals(materialId);
            case ExportOffer.Target.GoodType type -> goodTypes.of(materialId).contains(type.goodType());
        };
    }

    /** @return how many matching items the stacks hold */
    public static int count(ExportOffer offer, List<Stack> stacks, GoodTypes goodTypes) {
        return stacks.stream().filter(stack -> matches(offer.target(), stack.materialId(), goodTypes))
                .mapToInt(Stack::amount).sum();
    }

    /** @return what one batch takes, or empty when the stacks don't hold a whole batch */
    public static Optional<List<Take>> pick(ExportOffer offer, List<Stack> stacks, GoodTypes goodTypes) {
        List<Take> takes = new ArrayList<>();
        int needed = offer.batch();
        for (Stack stack : stacks) {
            if (needed == 0) {
                break;
            }
            if (stack.amount() > 0 && matches(offer.target(), stack.materialId(), goodTypes)) {
                int taken = Math.min(needed, stack.amount());
                takes.add(new Take(stack.slot(), stack.materialId(), taken));
                needed -= taken;
            }
        }
        return needed == 0 ? Optional.of(takes) : Optional.empty();
    }
}
