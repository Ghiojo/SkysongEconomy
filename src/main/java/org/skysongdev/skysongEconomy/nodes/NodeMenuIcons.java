package org.skysongdev.skysongEconomy.nodes;

import java.util.Objects;

/**
 * The items the node menu shows, as Minecraft item names such as {@code IRON_PICKAXE}. They are
 * checked when the menu is built; an unknown name falls back to stone with a warning.
 *
 * @param info       the node's details at the top
 * @param tier       a tier the player can gather at
 * @param lockedTier a tier they can't, such as one above their skill level
 * @param emptyPlot  a farm node's planting slot while nothing is planted
 * @param takeOut    the button that takes a farm node's primer out
 * @param booster    the booster slot while it is empty
 */
public record NodeMenuIcons(String info, String tier, String lockedTier, String emptyPlot, String takeOut,
                            String booster) {
    /** The icons used when {@code nodes/settings.yml} doesn't name them. */
    public static final NodeMenuIcons DEFAULTS = new NodeMenuIcons("BOOK", "IRON_PICKAXE", "GRAY_DYE", "DIRT", "SHEARS",
            "BLAZE_POWDER");

    public NodeMenuIcons {
        Objects.requireNonNull(info, "info");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(lockedTier, "lockedTier");
        Objects.requireNonNull(emptyPlot, "emptyPlot");
        Objects.requireNonNull(takeOut, "takeOut");
        Objects.requireNonNull(booster, "booster");
    }
}
