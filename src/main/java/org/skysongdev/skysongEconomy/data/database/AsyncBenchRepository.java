package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.bench.BenchRepository;
import org.skysongdev.skysongEconomy.business.bench.BusinessBench;

import java.util.List;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}, so tying or untying a bench never
 * waits for the database. Loading stays on the calling thread.
 */
public final class AsyncBenchRepository implements BenchRepository {
    private final BenchRepository delegate;
    private final BackgroundWriter writer;

    public AsyncBenchRepository(BenchRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<BusinessBench> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(BusinessBench bench) {
        writer.run("save the bench at " + bench.bench(), () -> delegate.save(bench));
    }

    @Override
    public void delete(BlockPosition bench) {
        writer.run("delete the bench at " + bench, () -> delegate.delete(bench));
    }
}
