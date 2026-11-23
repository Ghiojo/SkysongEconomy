package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.trade.pier.PierRepository;

import java.util.List;

/** Runs another repository's writes on a {@link BackgroundWriter}. Loading stays on the calling thread. */
public final class AsyncPierRepository implements PierRepository {
    private final PierRepository delegate;
    private final BackgroundWriter writer;

    public AsyncPierRepository(PierRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<Row> loadPiers() {
        return delegate.loadPiers();
    }

    @Override
    public List<ModifierRow> loadModifiers() {
        return delegate.loadModifiers();
    }

    @Override
    public void savePier(Row row) {
        writer.run("save the pier " + row.id(), () -> delegate.savePier(row));
    }

    @Override
    public void deletePier(String id) {
        writer.run("delete the pier " + id, () -> delegate.deletePier(id));
    }

    @Override
    public void saveModifier(ModifierRow row) {
        writer.run("save the loss modifier " + row.modifier().id() + " of the pier " + row.pierId(),
                () -> delegate.saveModifier(row));
    }

    @Override
    public void deleteModifier(String pierId, String modifierId) {
        writer.run("delete the loss modifier " + modifierId + " of the pier " + pierId,
                () -> delegate.deleteModifier(pierId, modifierId));
    }
}
