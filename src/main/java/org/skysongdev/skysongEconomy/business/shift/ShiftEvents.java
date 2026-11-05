package org.skysongdev.skysongEconomy.business.shift;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.character.CharacterRef;

/** Tells other plugins about shifts. On the server these are Bukkit events. */
public interface ShiftEvents {
    /**
     * A shift is about to start.
     *
     * @param message the business's shift message, placeholders filled in
     * @return {@code false} when a listener cancelled it
     */
    boolean starting(Business business, CharacterRef character, String message);

    /** A shift has ended; its pay can now be retrieved. */
    void ended(Business business, Shift shift);
}
