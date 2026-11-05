package org.skysongdev.skysongEconomy.business.activity;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.Objects;

/**
 * Something a player is busy with at a business: a shift or a training session.
 *
 * @param kind       what they are doing
 * @param businessId where
 * @param character  which of their characters is doing it
 */
public record Activity(Kind kind, String businessId, CharacterRef character) {
    /** The kinds of activity; a player account runs at most one at a time. */
    public enum Kind {
        SHIFT, TRAINING
    }

    public Activity {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(character, "character");
    }
}
