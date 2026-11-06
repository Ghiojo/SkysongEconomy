package org.skysongdev.skysongEconomy.gui;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Runs {@link Menu}s. Every click and drag while a menu is open is cancelled, so the server
 * never moves an item on its own. Clicks on the menu run its buttons. On an input slot, a click
 * with an item on the cursor offers that stack (a right-click offers one), a click with an
 * empty cursor takes back what the slot holds, and a shift-click in the player's inventory
 * offers that stack to the first input slot that takes it. Closing a menu gives back whatever
 * its input slots still hold.
 */
public final class MenuListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            clickMenu(event, menu, player);
        } else if (event.getClickedInventory() == event.getView().getBottomInventory() && event.isShiftClick()) {
            shiftIntoMenu(event, menu, player);
        }
    }

    private static void clickMenu(InventoryClickEvent event, Menu menu, Player player) {
        int slot = event.getSlot();
        ItemStack cursor = event.getView().getCursor();
        if (isEmpty(cursor) && menu.holds(slot)) {
            menu.giveBack(slot, player);
        } else if (!menu.isInputSlot(slot) || isEmpty(cursor)) {
            menu.click(slot, player, event.getClick());
        } else {
            boolean one = event.getClick() == ClickType.RIGHT && cursor.getAmount() > 1;
            ItemStack offered = one ? cursor.asQuantity(1) : cursor;
            ItemStack left = menu.offer(slot, player, offered);
            int taken = offered.getAmount() - (left == null ? 0 : left.getAmount());
            int remaining = cursor.getAmount() - taken;
            event.getView().setCursor(remaining > 0 ? cursor.asQuantity(remaining) : null);
        }
    }

    private static void shiftIntoMenu(InventoryClickEvent event, Menu menu, Player player) {
        ItemStack stack = event.getCurrentItem();
        if (isEmpty(stack)) {
            return;
        }
        for (int slot : menu.inputSlots()) {
            ItemStack left = menu.offer(slot, player, stack);
            if (left != stack) {
                event.getClickedInventory().setItem(event.getSlot(), left);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Menu menu && event.getPlayer() instanceof Player player) {
            menu.giveEverythingBack(player);
        }
    }

    /**
     * Closes every open menu, giving back what its input slots hold. Called when the plugin
     * stops, so nothing a player put in a menu is lost.
     */
    public static void closeAll(Server server) {
        for (Player player : server.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu menu) {
                menu.giveEverythingBack(player);
                player.closeInventory();
            }
        }
    }

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType().isAir() || stack.getAmount() <= 0;
    }
}
