package org.skysongdev.skysongEconomy.calendar;

import java.util.Locale;
import java.util.Objects;

/**
 * A season or month of the IC calendar, as SkysongCalendar names it.
 *
 * @param id          a lower-case id with no spaces, such as {@code vinse_er}
 * @param displayName what players read, such as {@code Vinse'er}
 */
public record CalendarName(String id, String displayName) {
    public CalendarName {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
    }

    /** @return the id SkysongEconomy uses for an enum constant's name, such as {@code VINSE_ER} to {@code vinse_er} */
    public static String idOf(String constantName) {
        return constantName.toLowerCase(Locale.ROOT);
    }

    /** @return whether {@code text} is this name's id or display name, ignoring case and surrounding spaces */
    public boolean matches(String text) {
        String written = text.strip();
        return id.equalsIgnoreCase(written) || displayName.equalsIgnoreCase(written);
    }
}
