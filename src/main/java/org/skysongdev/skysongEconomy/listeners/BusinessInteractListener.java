package org.skysongdev.skysongEconomy.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.gui.business.BusinessMenus;

/**
 * Opens a business's menu when its block is right-clicked. The block's own use (a chest, a
 * crafting table...) is cancelled, so the block is only a business.
 */
public final class BusinessInteractListener implements Listener {
    private final BusinessService businesses;
    private final BusinessMenus menus;

    public BusinessInteractListener(BusinessService businesses, BusinessMenus menus) {
        this.businesses = businesses;
        this.menus = menus;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || block == null) {
            return;
        }
        BlockPosition position = new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        businesses.at(position).ifPresent(business -> {
            event.setCancelled(true);
            menus.openMain(event.getPlayer(), business);
        });
    }
}
