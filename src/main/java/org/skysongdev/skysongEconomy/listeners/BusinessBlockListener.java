package org.skysongdev.skysongEconomy.listeners;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.List;
import java.util.Optional;

/**
 * Keeps business blocks in place. Players can't break them, and neither can explosions, fire,
 * pistons or mobs. Staff with {@code skysongeconomy.business.break} can: the business then
 * keeps all its data without a block until it is moved.
 */
public final class BusinessBlockListener implements Listener {
    /** Lets staff break business blocks. */
    public static final String BREAK_PERMISSION = "skysongeconomy.business.break";

    private final BusinessService businesses;
    private final Messenger messenger;

    public BusinessBlockListener(BusinessService businesses, Messenger messenger) {
        this.businesses = businesses;
        this.messenger = messenger;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        businessAt(event.getBlock()).ifPresent(business -> {
            if (!event.getPlayer().hasPermission(BREAK_PERMISSION)) {
                event.setCancelled(true);
                messenger.send(event.getPlayer(), "business.block.protected",
                        Placeholder.unparsed("business", business.displayName()));
                return;
            }
            businesses.unplace(business);
            messenger.send(event.getPlayer(), "business.block.unplaced",
                    Placeholder.unparsed("business", business.displayName()), Placeholder.unparsed("id", business.id()));
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        protect(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        protect(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (businessAt(event.getBlock()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> businessAt(block).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> businessAt(block).isPresent())) {
            event.setCancelled(true);
        }
    }

    /** Endermen, withers, falling blocks and the like. */
    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (businessAt(event.getBlock()).isPresent()) {
            event.setCancelled(true);
        }
    }

    private void protect(List<Block> exploded) {
        exploded.removeIf(block -> businessAt(block).isPresent());
    }

    private Optional<Business> businessAt(Block block) {
        return businesses.at(new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
    }
}
