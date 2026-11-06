package org.skysongdev.skysongEconomy.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.gui.node.NodeMenus;
import org.skysongdev.skysongEconomy.nodes.NodeService;

/** Right-clicking a node block opens the node's menu instead of using the block. */
public final class NodeInteractListener implements Listener {
    private final NodeService nodes;
    private final NodeMenus menus;

    public NodeInteractListener(NodeService nodes, NodeMenus menus) {
        this.nodes = nodes;
        this.menus = menus;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || block == null) {
            return;
        }
        BlockPosition position = new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        nodes.at(position).ifPresent(node -> {
            event.setCancelled(true);
            menus.open(event.getPlayer(), node);
        });
    }
}
