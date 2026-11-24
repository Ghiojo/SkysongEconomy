package org.skysongdev.skysongEconomy.business.shift;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;

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

    /**
     * A worker was paid for finished shifts.
     *
     * @param shifts          the shifts paid for
     * @param total           what was paid: money and tokens
     * @param businessDeleted whether a deleted business paid them without the worker collecting
     */
    default void paid(Business business, CharacterRef worker, List<Shift> shifts, ShiftPay total, boolean businessDeleted) {
    }
}
