package org.skysongdev.skysongEconomy.tokens;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.HashMap;
import java.util.Map;

/** Keeps saved balances in a map, so tests can check what was written. */
final class InMemoryTokenRepository implements TokenRepository {
    final Map<CharacterRef, Long> saved = new HashMap<>();

    @Override
    public Map<CharacterRef, Long> loadAll() {
        return new HashMap<>(saved);
    }

    @Override
    public void save(CharacterRef character, long balance) {
        saved.put(character, balance);
    }
}
