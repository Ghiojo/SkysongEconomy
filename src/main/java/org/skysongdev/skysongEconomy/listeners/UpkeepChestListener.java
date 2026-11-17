package org.skysongdev.skysongEconomy.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.upkeep.ChestMove;
import org.skysongdev.skysongEconomy.upkeep.UpkeepChests;

import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Keeps NPC upkeep chests to their rules: only items that pay the NPC's cost go in, and only
 * players with the NPC's {@code inventory-edit} action take items out. Every change is saved a
 * tick later, once the server has moved the items.
 */
public final class UpkeepChestListener implements Listener {
    private final Plugin plugin;
    private final UpkeepChests chests;
    private final BiPredicate<Player, UUID> mayTakeOut;
    private final Messenger messenger;

    /** @param mayTakeOut whether a player may take items out of an NPC's chest */
    public UpkeepChestListener(Plugin plugin, UpkeepChests chests, BiPredicate<Player, UUID> mayTakeOut,
                               Messenger messenger) {
        this.plugin = plugin;
        this.chests = chests;
        this.mayTakeOut = mayTakeOut;
        this.messenger = messenger;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof UpkeepChests.Chest chest)
                || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        boolean inChest = event.getClickedInventory() == event.getView().getTopInventory();
        ItemStack incoming = incoming(event, inChest, player);
        ChestMove move = move(event.getAction(), inChest);
        if (!move.allowed(mayTakeOut.test(player, chest.npc()), chest.accepts(incoming))) {
            event.setCancelled(true);
            boolean refusedItem = (move == ChestMove.PUT_IN || move == ChestMove.SWAP) && !chest.accepts(incoming);
            messenger.send(player, refusedItem ? "upkeep.chest-refused-item" : "upkeep.chest-deposit-only");
            return;
        }
        if (move != ChestMove.NONE) {
            saveLater(chest);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof UpkeepChests.Chest chest)
                || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int chestSize = event.getView().getTopInventory().getSize();
        boolean touchesChest = event.getRawSlots().stream().anyMatch(slot -> slot < chestSize);
        if (!touchesChest) {
            return;
        }
        if (!chest.accepts(event.getOldCursor())) {
            event.setCancelled(true);
            messenger.send(player, "upkeep.chest-refused-item");
            return;
        }
        saveLater(chest);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof UpkeepChests.Chest chest) {
            chests.closed(chest, event.getPlayer());
        }
    }

    private void saveLater(UpkeepChests.Chest chest) {
        plugin.getServer().getScheduler().runTask(plugin, chest::save);
    }

    /** @return the item that would go into the chest, or null when none does */
    private static ItemStack incoming(InventoryClickEvent event, boolean inChest, Player player) {
        return switch (event.getAction()) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> inChest ? event.getCursor() : null;
            case MOVE_TO_OTHER_INVENTORY -> inChest ? null : event.getCurrentItem();
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                if (!inChest) {
                    yield null;
                }
                int button = event.getHotbarButton();
                Inventory own = player.getInventory();
                yield button >= 0 ? own.getItem(button) : player.getInventory().getItemInOffHand();
            }
            default -> null;
        };
    }

    /** Sorts an inventory action by what it does to the chest. */
    static ChestMove move(InventoryAction action, boolean inChest) {
        return switch (action) {
            case NOTHING, CLONE_STACK -> ChestMove.NONE;
            case PLACE_ALL, PLACE_ONE, PLACE_SOME -> inChest ? ChestMove.PUT_IN : ChestMove.NONE;
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME, DROP_ALL_SLOT, DROP_ONE_SLOT ->
                    inChest ? ChestMove.TAKE_OUT : ChestMove.NONE;
            case SWAP_WITH_CURSOR -> inChest ? ChestMove.SWAP : ChestMove.NONE;
            case MOVE_TO_OTHER_INVENTORY -> inChest ? ChestMove.TAKE_OUT : ChestMove.PUT_IN;
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> inChest ? ChestMove.SWAP : ChestMove.NONE;
            case DROP_ALL_CURSOR, DROP_ONE_CURSOR -> ChestMove.NONE;
            // Collecting to the cursor can pull matching items out of the chest; unknown actions
            // (such as newer bundle clicks) are treated the same way.
            default -> ChestMove.OTHER;
        };
    }
}
