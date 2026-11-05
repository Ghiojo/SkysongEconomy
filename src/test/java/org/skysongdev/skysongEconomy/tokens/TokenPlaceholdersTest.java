package org.skysongdev.skysongEconomy.tokens;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenPlaceholdersTest {
    private final UUID steve = UUID.randomUUID();
    private final UUID alex = UUID.randomUUID();
    private final CharacterRef steveCharacter = new CharacterRef(steve, "Aldric");
    private TokenPlaceholders placeholders;

    @BeforeEach
    void setUp() {
        GatheringTokens tokens = new GatheringTokens(new InMemoryTokenRepository());
        tokens.add(steveCharacter, 12);
        tokens.add(new CharacterRef(steve, "Other"), 99);
        CharacterDirectory characters = new CharacterDirectory() {
            @Override
            public Optional<CharacterRef> active(UUID player) {
                return player.equals(steve) ? Optional.of(steveCharacter) : Optional.empty();
            }

            @Override
            public List<CharacterRef> all(UUID player) {
                return active(player).stream().toList();
            }

            @Override
            public boolean exists(CharacterRef character) {
                return true;
            }
        };
        Map<String, UUID> players = Map.of("steve", steve, "alex", alex);
        placeholders = new TokenPlaceholders(tokens, characters,
                name -> Optional.ofNullable(players.get(name.toLowerCase())));
    }

    @Test
    void showsTheViewersActiveCharacter() {
        assertEquals(Optional.of("12"), placeholders.resolve(steve, "tokens"));
    }

    @Test
    void showsANamedPlayersActiveCharacter() {
        assertEquals(Optional.of("12"), placeholders.resolve(alex, "tokens_Steve"));
    }

    @Test
    void showsZeroWithoutACharacterOrPlayer() {
        assertEquals(Optional.of("0"), placeholders.resolve(alex, "tokens"));
        assertEquals(Optional.of("0"), placeholders.resolve(null, "tokens"));
        assertEquals(Optional.of("0"), placeholders.resolve(steve, "tokens_nobody"));
    }

    @Test
    void leavesOtherPlaceholdersAlone() {
        assertEquals(Optional.empty(), placeholders.resolve(steve, "coins"));
        assertEquals(Optional.empty(), placeholders.resolve(steve, "tokens_"));
    }
}
