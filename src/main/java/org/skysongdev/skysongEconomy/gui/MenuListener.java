package org.skysongdev.skysongEconomy.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Makes {@link Menu}s read-only: every click and drag while a menu is open is cancelled, so
 * items can't be taken out or put in. Clicks on the menu itself run its buttons, and clicks on
 * the player's own inventory go to {@link Menu#clickedOwnInventory}.
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
            menu.click(event.getSlot(), player, event.getClick());
        } else if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            menu.clickOwn(event.getSlot(), player, event.getClick());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
