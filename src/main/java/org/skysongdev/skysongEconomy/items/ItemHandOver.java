package org.skysongdev.skysongEconomy.items;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Hands Skysong items to a player: into their inventory, and whatever doesn't fit at their feet.
 * Stacks are split to each item's stack size, so nothing oversized is ever dropped.
 */
public final class ItemHandOver {
    private final SkysongItemFactory items;

    public ItemHandOver(SkysongItemFactory items) {
        this.items = Objects.requireNonNull(items, "items");
    }

    /**
     * Builds every stack first, so a missing item file stops the hand-over before anything is given.
     *
     * @return whether some items didn't fit and were dropped at the player's feet
     * @throws IllegalArgumentException when an item has no file
     */
    public boolean give(Player player, List<ItemAmount> amounts) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemAmount item : amounts) {
            int maxStack = Math.max(1, items.create(item.materialId(), 1).getMaxStackSize());
            for (int left = item.amount(); left > 0; left -= maxStack) {
                stacks.add(items.create(item.materialId(), Math.min(left, maxStack)));
            }
        }
        return giveStacks(player, stacks);
    }

    /**
     * Puts the stacks in the player's inventory as they are, such as primers given back.
     *
     * @return whether some didn't fit and were dropped at the player's feet
     */
    public boolean giveStacks(Player player, List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return false;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stacks.toArray(ItemStack[]::new));
        leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        return !leftover.isEmpty();
    }

    /** @return the item's name as players see it; its material id when it has no file or no name */
    public Component name(String materialId) {
        if (!items.exists(materialId)) {
            return Component.text(materialId);
        }
        ItemMeta meta = items.create(materialId, 1).getItemMeta();
        Component name = meta == null ? null : meta.displayName();
        return name == null ? Component.text(materialId) : name;
    }
}
