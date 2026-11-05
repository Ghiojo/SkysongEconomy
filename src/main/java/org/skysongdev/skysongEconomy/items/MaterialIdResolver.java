package org.skysongdev.skysongEconomy.items;

import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Reads the Skysong Items {@code material_id} tag, which identifies every Skysong item in the
 * price ledger and good type tables.
 */
public final class MaterialIdResolver {
    /** The key Skysong Items stores every item's material ID under ({@code skysong_items:material_id}). */
    public static final NamespacedKey SKYSONG_ITEMS_KEY = new NamespacedKey("skysong_items", "material_id");

    private final NamespacedKey materialIdKey;

    public MaterialIdResolver(NamespacedKey materialIdKey) {
        this.materialIdKey = materialIdKey;
    }

    public Optional<String> fromItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer().get(materialIdKey, PersistentDataType.STRING));
    }

    /** The material ID of the item in the sender's main hand; empty for the console or an untagged item. */
    public Optional<String> fromMainHand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return Optional.empty();
        }
        return fromItem(player.getInventory().getItemInMainHand());
    }

    /**
     * Resolves the material ID for commands shaped like {@code [materialId] <arg> ...}: when the
     * sender typed more than {@code argsAfterId} arguments the first one is the ID, otherwise the
     * ID comes from the item in their main hand.
     */
    public Optional<String> fromArgsOrMainHand(CommandSender sender, String[] args, int argsAfterId) {
        if (args.length > argsAfterId) {
            return Optional.of(args[0]);
        }
        return fromMainHand(sender);
    }
}
