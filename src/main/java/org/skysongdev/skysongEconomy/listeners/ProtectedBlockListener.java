package org.skysongdev.skysongEconomy.listeners;

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
import org.skysongdev.skysongEconomy.blocks.BlockGuard;
import org.skysongdev.skysongEconomy.blocks.GuardedBlock;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.List;
import java.util.Optional;

/**
 * Keeps guarded blocks, such as business and node blocks, in place. Players can't break them,
 * and neither can explosions, fire, pistons or mobs. Staff with the block's break permission
 * can: its feature then keeps all its data without a block until it is moved.
 */
public final class ProtectedBlockListener implements Listener {
    private final List<BlockGuard> guards;
    private final Messenger messenger;

    public ProtectedBlockListener(List<BlockGuard> guards, Messenger messenger) {
        this.guards = List.copyOf(guards);
        this.messenger = messenger;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        guarded(event.getBlock()).ifPresent(block -> {
            if (!event.getPlayer().hasPermission(block.breakPermission())) {
                event.setCancelled(true);
                messenger.send(event.getPlayer(), block.protectedMessage(), Messenger.unparsed(block.placeholders()));
                return;
            }
            block.unplace().run();
            messenger.send(event.getPlayer(), block.unplacedMessage(), Messenger.unparsed(block.placeholders()));
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
        if (guarded(event.getBlock()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> guarded(block).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> guarded(block).isPresent())) {
            event.setCancelled(true);
        }
    }

    /** Endermen, withers, falling blocks and the like. */
    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (guarded(event.getBlock()).isPresent()) {
            event.setCancelled(true);
        }
    }

    private void protect(List<Block> exploded) {
        exploded.removeIf(block -> guarded(block).isPresent());
    }

    private Optional<GuardedBlock> guarded(Block block) {
        BlockPosition position = new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        for (BlockGuard guard : guards) {
            Optional<GuardedBlock> held = guard.at(position);
            if (held.isPresent()) {
                return held;
            }
        }
        return Optional.empty();
    }
}
