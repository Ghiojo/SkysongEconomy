package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.nodes.farm.PrimerReturn;
import org.skysongdev.skysongEconomy.nodes.farm.PrimerReturnRepository;

import java.util.List;
import java.util.UUID;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}. Loading stays on the calling
 * thread because the plugin needs the data before it can start.
 */
public final class AsyncPrimerReturnRepository implements PrimerReturnRepository {
    private final PrimerReturnRepository delegate;
    private final BackgroundWriter writer;

    public AsyncPrimerReturnRepository(PrimerReturnRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<PrimerReturn> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(PrimerReturn waiting) {
        writer.run("save the waiting primer " + waiting.id(), () -> delegate.save(waiting));
    }

    @Override
    public void delete(UUID id) {
        writer.run("delete the waiting primer " + id, () -> delegate.delete(id));
    }
}
