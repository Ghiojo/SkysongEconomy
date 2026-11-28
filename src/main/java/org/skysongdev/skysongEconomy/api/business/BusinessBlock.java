package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.block.Block;

import java.util.Objects;

/**
 * The block a business is opened from: the world's name and block coordinates. A block holds at
 * most one business.
 *
 * @param world the world's name
 * @param x     block x
 * @param y     block y
 * @param z     block z
 * @since 1.1.6
 */
public record BusinessBlock(String world, int x, int y, int z) {
    public BusinessBlock {
        Objects.requireNonNull(world, "world");
    }

    /** @return the position of that block */
    public static BusinessBlock of(Block block) {
        return new BusinessBlock(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    /** @return whether the business is on the block at those coordinates */
    public boolean isAt(String world, int x, int y, int z) {
        return this.world.equals(world) && this.x == x && this.y == y && this.z == z;
    }

    @Override
    public String toString() {
        return world + " " + x + " " + y + " " + z;
    }
}
