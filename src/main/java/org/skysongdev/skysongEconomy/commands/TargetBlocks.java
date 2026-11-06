package org.skysongdev.skysongEconomy.commands;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.Optional;

/** The block a player is looking at, for commands that place something on it. */
public final class TargetBlocks {
    /** How far away the block may be, in blocks. */
    private static final int RANGE = 5;

    private TargetBlocks() {
    }

    /**
     * @return the block the player is looking at, within reach; when there is none the player is
     *         told to look at one
     */
    public static Optional<BlockPosition> target(Player player, Messenger messenger) {
        Block block = player.getTargetBlockExact(RANGE);
        if (block == null || block.getType().isAir()) {
            messenger.send(player, "command.look-at-block");
            return Optional.empty();
        }
        return Optional.of(new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
    }
}
