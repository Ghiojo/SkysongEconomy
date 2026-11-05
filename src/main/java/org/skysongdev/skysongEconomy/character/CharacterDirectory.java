package org.skysongdev.skysongEconomy.character;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Looks up the characters a player account has. */
public interface CharacterDirectory {
    /**
     * @return the character the player is playing right now, empty when they have none
     */
    Optional<CharacterRef> active(UUID player);

    /**
     * @return every character of the player, empty when they have none
     */
    List<CharacterRef> all(UUID player);

    /**
     * @return whether the character still exists
     */
    boolean exists(CharacterRef character);
}
