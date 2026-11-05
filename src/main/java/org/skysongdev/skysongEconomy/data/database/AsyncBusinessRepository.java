package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.BusinessRepository;
import org.skysongdev.skysongEconomy.business.BusinessSnapshot;

import java.util.List;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}. Loading stays on the calling
 * thread because the plugin needs the data before it can start.
 */
public final class AsyncBusinessRepository implements BusinessRepository {
    private final BusinessRepository delegate;
    private final BackgroundWriter writer;

    public AsyncBusinessRepository(BusinessRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<BusinessSnapshot> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(BusinessSnapshot business) {
        writer.run("save business " + business.id(), () -> delegate.save(business));
    }

    @Override
    public void delete(String id) {
        writer.run("delete business " + id, () -> delegate.delete(id));
    }
}
