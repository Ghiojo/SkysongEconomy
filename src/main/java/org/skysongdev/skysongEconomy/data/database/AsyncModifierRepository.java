package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.prices.modifier.ModifierRepository;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;

import java.util.List;

/** Runs another repository's writes on a {@link BackgroundWriter}. Loading stays on the calling thread. */
public final class AsyncModifierRepository implements ModifierRepository {
    private final ModifierRepository delegate;
    private final BackgroundWriter writer;

    public AsyncModifierRepository(ModifierRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<PriceModifier> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(PriceModifier modifier) {
        writer.run("save price modifier " + modifier.id(), () -> delegate.save(modifier));
    }

    @Override
    public void delete(String id) {
        writer.run("delete price modifier " + id, () -> delegate.delete(id));
    }
}
