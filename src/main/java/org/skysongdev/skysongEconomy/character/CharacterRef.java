package org.skysongdev.skysongEconomy.character;

import java.util.Objects;
import java.util.UUID;

/**
 * One roleplay character: a player account plus the name of one of its SkysongStats profiles.
 * <p>
 * Businesses are owned and worked by characters, not by accounts, so every business feature
 * identifies people with this type. It deliberately mirrors SkysongStats' {@code CharacterId}
 * without depending on it, so the business rules can be unit tested without the server.
 *
 * @param player  the account that owns the character
 * @param profile the character's profile name, as SkysongStats stores it
 */
public record CharacterRef(UUID player, String profile) {
    public CharacterRef {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(profile, "profile");
        if (profile.isBlank()) {
            throw new IllegalArgumentException("profile must not be blank");
        }
    }
}
