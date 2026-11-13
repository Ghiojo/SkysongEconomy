package org.skysongdev.skysongEconomy.business.npc;

import java.util.Locale;
import java.util.Optional;

/** Where a business's new NPC appears, as {@code spawn-at} in {@code businesses/npcs.yml} says. */
public enum SpawnAt {
    /** On a free spot in front of whoever creates it. */
    PLAYER,
    /** On a free spot next to the business block. */
    BUSINESS;

    /** @return the name used in config, such as {@code player} */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the place with that config name */
    public static Optional<SpawnAt> fromKey(String key) {
        for (SpawnAt place : values()) {
            if (place.key().equalsIgnoreCase(key.trim())) {
                return Optional.of(place);
            }
        }
        return Optional.empty();
    }
}
