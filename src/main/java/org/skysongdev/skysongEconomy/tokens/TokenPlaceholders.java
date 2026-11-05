package org.skysongdev.skysongEconomy.tokens;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * The text of the token placeholders, without PlaceholderAPI itself so it can be tested:
 * <ul>
 *   <li>{@code %skysongeconomy_tokens%}: the viewer's active character</li>
 *   <li>{@code %skysongeconomy_tokens_<player>%}: that player's active character</li>
 * </ul>
 * A player who isn't playing a character shows 0.
 */
public final class TokenPlaceholders {
    private static final String TOKENS = "tokens";
    private static final String OF_PLAYER = TOKENS + "_";

    private final GatheringTokens tokens;
    private final CharacterDirectory characters;
    private final Function<String, Optional<UUID>> playerByName;

    /** @param playerByName finds a player's id by name, online or not */
    public TokenPlaceholders(GatheringTokens tokens, CharacterDirectory characters,
                             Function<String, Optional<UUID>> playerByName) {
        this.tokens = tokens;
        this.characters = characters;
        this.playerByName = playerByName;
    }

    /**
     * @param viewer the player the text is for, {@code null} when there is none (e.g. the console)
     * @param params what follows {@code skysongeconomy_}, e.g. {@code tokens_Steve}
     * @return the text, or empty when {@code params} is not one of these placeholders
     */
    public Optional<String> resolve(@Nullable UUID viewer, String params) {
        String lower = params.toLowerCase(Locale.ROOT);
        if (lower.equals(TOKENS)) {
            return Optional.of(balanceOf(viewer));
        }
        if (lower.startsWith(OF_PLAYER) && lower.length() > OF_PLAYER.length()) {
            return Optional.of(balanceOf(playerByName.apply(params.substring(OF_PLAYER.length())).orElse(null)));
        }
        return Optional.empty();
    }

    private String balanceOf(@Nullable UUID player) {
        if (player == null) {
            return "0";
        }
        return String.valueOf(characters.active(player).map(tokens::balance).orElse(0L));
    }
}
