package org.skysongdev.skysongEconomy.items;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Turns item stacks into {@link StoredItems} and back, with Paper's own item format, so every
 * name, lore line and tag survives a restart and a server update.
 */
public final class ItemStackBytes {
    private ItemStackBytes() {
    }

    /** @param stacks at least one stack, none of them air */
    public static StoredItems store(List<ItemStack> stacks) {
        return StoredItems.of(stacks.stream().map(ItemStack::serializeAsBytes).toList());
    }

    /** @return new copies of the stored stacks */
    public static List<ItemStack> restore(StoredItems items) {
        return items.stacks().stream().map(ItemStack::deserializeBytes).toList();
    }
}
