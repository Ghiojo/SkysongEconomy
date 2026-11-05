package org.skysongdev.skysongEconomy.business;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Instant;
import java.util.Objects;

/**
 * A character working at a business, and when they were hired. The hire date decides who
 * leaves first when a downgrade leaves too few worker slots: the newest hires go first.
 */
public record Hire(CharacterRef character, Instant hiredAt) {
    public Hire {
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(hiredAt, "hiredAt");
    }
}
