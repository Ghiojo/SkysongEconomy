package org.skysongdev.skysongEconomy.integration.skysongitems;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.upkeep.ItemMark;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepEvents;
import org.skysongdev.skysongEconomy.upkeep.UpkeepService;
import org.skysongdev.skysongEconomy.upkeep.UpkeepState;
import org.skysongdev.skysongitems.api.SkysongItemsApi;
import org.skysongdev.skysongitems.api.TagType;
import org.skysongdev.skysongitems.api.TraitDefinition;
import org.skysongdev.skysongitems.api.event.SkysongItemsRegisterEvent;
import org.skysongdev.skysongnpcs.api.items.NpcItems;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Keeps NPC items in line with their NPC's upkeep: the item of an unmaintained NPC gets the
 * {@code unmaintained} trait, printed in its lore, and taking that trait off (with a
 * SkysongCrafting recipe, or {@code /si trait remove}) repairs the NPC and restarts its timer.
 * See {@link ItemMark} for how a removal is told apart from an item that was never marked.
 * <p>
 * Only an NPC's current item (the one that can place it) counts. Must be used on the server's
 * main thread.
 */
public final class UpkeepItemMarks implements Listener {
    /** The trait an unmaintained NPC's item carries; staff use it in the repair recipe. */
    public static final String TRAIT = "unmaintained";
    /** The hidden tag that is set together with the trait. */
    public static final String MARKED_TAG = "upkeep_marked";

    private final Plugin plugin;
    private final UpkeepService upkeep;
    private final NpcItems npcItems;
    private final Supplier<SkysongItemsApi> items;
    private final TraitText text;
    private final Logger logger;

    /**
     * The trait's texts, MiniMessage from {@code messages.yml}.
     *
     * @param lore        the line printed on the item
     * @param name        the trait's name in SkysongItems' menus
     * @param description the trait's description in SkysongItems' menus
     */
    public record TraitText(String lore, String name, String description) {
    }

    /**
     * @param items the SkysongItems API, looked up on every use so a reload of it is seen
     */
    public UpkeepItemMarks(Plugin plugin, UpkeepService upkeep, NpcItems npcItems,
                           Supplier<SkysongItemsApi> items, TraitText text, Logger logger) {
        this.plugin = plugin;
        this.upkeep = upkeep;
        this.npcItems = npcItems;
        this.items = items;
        this.text = text;
        this.logger = logger;
    }

    /** Registers the trait and its tag each time SkysongItems loads them. */
    @EventHandler
    public void onRegister(SkysongItemsRegisterEvent event) {
        if (!event.registerTag(plugin, MARKED_TAG, TagType.BOOL)) {
            logger.warning("SkysongItems refused the tag " + MARKED_TAG + ": unmaintained NPC items can't be repaired");
        }
        TraitDefinition trait = TraitDefinition.printable(TRAIT, text.lore(), List.of())
                .name(text.name()).description(text.description()).category("NPC");
        if (!event.registerTrait(plugin, trait)) {
            logger.warning("SkysongItems refused the trait " + TRAIT + ": unmaintained NPC items won't show it");
        }
    }

    /** Brings every NPC item in the player's own inventory in line. */
    public void sync(Player player) {
        sync(player.getInventory(), player);
    }

    /**
     * Brings every NPC item in an inventory in line.
     *
     * @param holder the player named in SkysongItems' change events, or {@code null}
     */
    public void sync(Inventory inventory, @Nullable Player holder) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.isEmpty()) {
                continue;
            }
            ItemStack copy = item.clone();
            if (sync(copy, holder)) {
                inventory.setItem(slot, copy);
            }
        }
    }

    /**
     * Brings one item in line: marks it, unmarks it, or repairs its NPC.
     *
     * @return whether the item was changed, so the caller puts it back
     */
    public boolean sync(ItemStack item, @Nullable Player holder) {
        Optional<UUID> npc = currentNpc(item);
        if (npc.isEmpty()) {
            return false;
        }
        SkysongItemsApi api = items.get();
        boolean hasTrait = api.traits().has(item, TRAIT);
        boolean marked = api.tags().getBool(item, MARKED_TAG).orElse(false);
        UpkeepState state = upkeep.upkeep(npc.get()).map(NpcUpkeep::state).orElse(null);
        try {
            switch (ItemMark.decide(state, hasTrait, marked)) {
                case NONE -> {
                    return false;
                }
                case MARK -> {
                    api.traits().add(holder, item, TRAIT);
                    api.tags().set(holder, item, MARKED_TAG, true);
                }
                case UNMARK -> {
                    api.traits().remove(holder, item, TRAIT);
                    api.tags().remove(holder, item, MARKED_TAG);
                }
                case REPAIR -> {
                    upkeep.mark(npc.get(), UpkeepState.MAINTAINED, UpkeepEvents.Cause.REPAIR);
                    api.tags().remove(holder, item, MARKED_TAG);
                }
            }
            return true;
        } catch (IllegalArgumentException e) {
            // Not a SkysongItems item, or the trait was not registered (see onRegister).
            logger.fine("Could not mark the item of the NPC " + npc.get() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Repairs the NPC an item places, when the item's trait was taken off, before the item is
     * used up. Changes nothing on the item itself.
     */
    public void checkRepair(ItemStack item) {
        Optional<UUID> npc = currentNpc(item);
        if (npc.isEmpty()) {
            return;
        }
        SkysongItemsApi api = items.get();
        UpkeepState state = upkeep.upkeep(npc.get()).map(NpcUpkeep::state).orElse(null);
        if (ItemMark.decide(state, api.traits().has(item, TRAIT),
                api.tags().getBool(item, MARKED_TAG).orElse(false)) == ItemMark.REPAIR) {
            upkeep.mark(npc.get(), UpkeepState.MAINTAINED, UpkeepEvents.Cause.REPAIR);
        }
    }

    private Optional<UUID> currentNpc(ItemStack item) {
        if (item == null || item.isEmpty() || !npcItems.isCurrent(item)) {
            return Optional.empty();
        }
        return npcItems.storedNpc(item);
    }
}
