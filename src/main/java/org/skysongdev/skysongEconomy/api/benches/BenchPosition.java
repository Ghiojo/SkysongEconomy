package org.skysongdev.skysongEconomy.api.benches;

import org.bukkit.block.Block;

import java.util.Objects;

/**
 * A bench's block: the world's name and block coordinates. SkysongCrafting has at most one bench
 * per block, so this is what identifies a bench.
 */
public record BenchPosition(String world, int x, int y, int z) {
    public BenchPosition {
        Objects.requireNonNull(world, "world");
    }

    /** @return the position of that block */
    public static BenchPosition of(Block block) {
        return new BenchPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    @Override
    public String toString() {
        return world + " " + x + " " + y + " " + z;
    }
}
