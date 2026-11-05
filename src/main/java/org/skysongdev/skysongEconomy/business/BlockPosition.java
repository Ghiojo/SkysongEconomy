package org.skysongdev.skysongEconomy.business;

import java.util.Objects;

/**
 * The block a business is opened from: a world name and block coordinates.
 *
 * @param world the world's name
 */
public record BlockPosition(String world, int x, int y, int z) {
    public BlockPosition {
        Objects.requireNonNull(world, "world");
    }

    @Override
    public String toString() {
        return world + " " + x + " " + y + " " + z;
    }
}
