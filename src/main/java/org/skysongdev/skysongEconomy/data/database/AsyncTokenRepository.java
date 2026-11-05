package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.tokens.TokenRepository;

import java.util.Map;

/** Runs another repository's writes on a {@link BackgroundWriter}. Loading stays on the calling thread. */
public final class AsyncTokenRepository implements TokenRepository {
    private final TokenRepository delegate;
    private final BackgroundWriter writer;

    public AsyncTokenRepository(TokenRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public Map<CharacterRef, Long> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(CharacterRef character, long balance) {
        writer.run("save the Gathering Tokens of " + character, () -> delegate.save(character, balance));
    }
}
