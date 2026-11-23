package org.skysongdev.skysongEconomy.api.piers;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * How a pier looks in menus.
 *
 * @param displayName MiniMessage name, such as {@code <aqua>Harbour Pier}
 * @param icon        Minecraft material of its button, such as {@code OAK_BOAT}; an unknown one shows as paper
 * @param description MiniMessage lines under its name
 */
public record PierDetails(String displayName, String icon, List<String> description) {
    public PierDetails {
        Objects.requireNonNull(displayName, "displayName");
        icon = Objects.requireNonNull(icon, "icon").strip().toUpperCase(Locale.ROOT);
        description = List.copyOf(description);
    }
}
