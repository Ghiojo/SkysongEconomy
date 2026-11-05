package org.skysongdev.skysongEconomy.business.display;

/**
 * How the floating name above business blocks looks, from {@code businesses/block.yml}.
 *
 * @param enabled       whether names are shown at all
 * @param height        how far above the block's top the text floats, in blocks
 * @param viewDistance  how far away players can see it, in blocks
 * @param shadow        whether the text has a shadow
 * @param background    whether the text has the default dark background
 */
public record NameplateSettings(boolean enabled, double height, int viewDistance, boolean shadow, boolean background) {
    /** Minecraft's base entity view distance, which a display's view range multiplies. */
    private static final float BASE_VIEW_DISTANCE = 64f;

    public NameplateSettings {
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative: " + height);
        }
        if (viewDistance < 1) {
            throw new IllegalArgumentException("viewDistance must be at least 1: " + viewDistance);
        }
    }

    /** @return the view distance as the multiplier text displays use */
    public float viewRange() {
        return viewDistance / BASE_VIEW_DISTANCE;
    }
}
