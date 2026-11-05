package org.skysongdev.skysongEconomy.tokens;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.Map;

/** Where Gathering Token balances are saved. */
public interface TokenRepository {
    /** @return every saved balance; a character without one has 0 */
    Map<CharacterRef, Long> loadAll();

    /** Saves one character's balance. */
    void save(CharacterRef character, long balance);
}
