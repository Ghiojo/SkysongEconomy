package org.skysongdev.skysongEconomy.items;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongitems.api.ItemTags;

import java.util.Optional;

/**
 * Reads the Skysong Items {@code material_id} tag, which identifies every Skysong item in the
 * price ledger and good type tables. Reads go through the SkysongItems API, so they must run on
 * the server's main thread.
 */
public final class MaterialIdResolver {
    /** The id of the built-in SkysongItems string tag that holds an item's material ID. */
    static final String MATERIAL_ID_TAG = "material_id";

    private final ItemTags tags;

    /** @param tags the SkysongItems tag API, from {@code SkysongItemsApi.get().tags()} */
    public MaterialIdResolver(ItemTags tags) {
        this.tags = tags;
    }

    /** The item's material ID; empty for null, air or an item without one. */
    public Optional<String> fromItem(ItemStack item) {
        return tags.getString(item, MATERIAL_ID_TAG);
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
