package org.skysongdev.skysongEconomy.items;

import org.bukkit.inventory.ItemStack;

/** Creates Skysong items by material ID, so callers don't depend on how Skysong_Items stores them. */
public interface SkysongItemFactory {
    /**
     * @throws IllegalArgumentException if no item with that material ID exists
     */
    ItemStack create(String materialId, int amount);
}
