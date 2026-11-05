package org.skysongdev.skysongEconomy.gui.layout;

import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.Map;
import java.util.Objects;

/**
 * Which item each shared menu button uses, by Minecraft material name, from {@code menus.yml}.
 * Kept as names so the file can be checked without a server; the menus turn them into items.
 */
public record MenuIcons(String border, String previousPage, String nextPage, String back, String close,
                        String confirm, String cancel) {
    /** File name used in error messages and when loading. */
    public static final String FILE = "menus.yml";

    private static final Map<String, String> DEFAULTS = Map.of(
            "border", "GRAY_STAINED_GLASS_PANE",
            "previous-page", "ARROW",
            "next-page", "ARROW",
            "back", "DARK_OAK_DOOR",
            "close", "BARRIER",
            "confirm", "LIME_CONCRETE",
            "cancel", "RED_CONCRETE");

    public MenuIcons {
        Objects.requireNonNull(border, "border");
        Objects.requireNonNull(previousPage, "previousPage");
        Objects.requireNonNull(nextPage, "nextPage");
        Objects.requireNonNull(back, "back");
        Objects.requireNonNull(close, "close");
        Objects.requireNonNull(confirm, "confirm");
        Objects.requireNonNull(cancel, "cancel");
    }

    /** Reads the {@code icons} section; missing keys keep their defaults. */
    public static MenuIcons parse(ConfigSection root) {
        ConfigSection icons = root.section("icons");
        return new MenuIcons(read(icons, "border"), read(icons, "previous-page"), read(icons, "next-page"),
                read(icons, "back"), read(icons, "close"), read(icons, "confirm"), read(icons, "cancel"));
    }

    private static String read(ConfigSection icons, String key) {
        return icons.string(key, DEFAULTS.get(key)).toUpperCase(java.util.Locale.ROOT);
    }
}
