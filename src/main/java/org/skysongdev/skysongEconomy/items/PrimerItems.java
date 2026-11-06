package org.skysongdev.skysongEconomy.items;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.listeners.NodeItemTags;
import org.skysongdev.skysongitems.api.ItemTags;

import java.util.Locale;
import java.util.Optional;

/** Reads what makes an item a farm primer: its {@code node_primer} tag, or else its material id. */
public final class PrimerItems {
    private final ItemTags tags;
    private final MaterialIdResolver materialIds;

    /** @param tags the SkysongItems tag API, from {@code SkysongItemsApi.get().tags()} */
    public PrimerItems(ItemTags tags, MaterialIdResolver materialIds) {
        this.tags = tags;
        this.materialIds = materialIds;
    }

    /** @return the farm resource id in the item's {@code node_primer} tag; empty when it has none */
    public Optional<String> primerTag(ItemStack item) {
        return tags.getString(item, NodeItemTags.PRIMER).map(String::strip).filter(id -> !id.isEmpty())
                .map(id -> id.toLowerCase(Locale.ROOT));
    }

    /** @return the item's material id; empty when it has none */
    public Optional<String> materialId(ItemStack item) {
        return materialIds.fromItem(item);
    }
}
