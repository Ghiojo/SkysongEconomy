package org.skysongdev.skysongEconomy.items;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A plan to take a number of one item out of an inventory: the clicked stack first, then other
 * stacks just like it. Planning changes nothing, so a refusal in between leaves the inventory as
 * it was; {@link #takeFrom} then takes exactly what was planned.
 */
public final class StackTaking {
    private record Part(int slot, int amount) {
    }

    private final List<Part> parts;
    private final List<ItemStack> stacks;

    private StackTaking(List<Part> parts, List<ItemStack> stacks) {
        this.parts = List.copyOf(parts);
        this.stacks = List.copyOf(stacks);
    }

    /**
     * @param firstSlot the slot of the clicked stack
     * @param amount    how many to take, 1 or more
     * @return the plan; {@link #isEmpty()} when the inventory doesn't hold that many
     */
    public static StackTaking plan(Inventory inventory, int firstSlot, int amount) {
        ItemStack first = inventory.getItem(firstSlot);
        if (first == null || first.getType().isAir()) {
            return new StackTaking(List.of(), List.of());
        }
        List<Integer> slots = new ArrayList<>();
        slots.add(firstSlot);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (slot != firstSlot && stack != null && stack.isSimilar(first)) {
                slots.add(slot);
            }
        }
        List<Part> parts = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        int left = amount;
        for (int slot : slots) {
            if (left == 0) {
                break;
            }
            ItemStack stack = inventory.getItem(slot);
            int take = Math.min(left, stack.getAmount());
            parts.add(new Part(slot, take));
            stacks.add(stack.asQuantity(take));
            left -= take;
        }
        return left > 0 ? new StackTaking(List.of(), List.of()) : new StackTaking(parts, stacks);
    }

    /** @return whether the inventory didn't hold enough, so nothing can be taken */
    public boolean isEmpty() {
        return parts.isEmpty();
    }

    /** @return copies of what will be taken, one per stack it comes from */
    public List<ItemStack> stacks() {
        return stacks.stream().map(ItemStack::clone).toList();
    }

    /** Takes what was planned out of the inventory. */
    public void takeFrom(Inventory inventory) {
        for (Part part : parts) {
            ItemStack stack = inventory.getItem(part.slot());
            if (stack == null) {
                continue;
            }
            int left = stack.getAmount() - part.amount();
            inventory.setItem(part.slot(), left > 0 ? stack.asQuantity(left) : null);
        }
    }
}
