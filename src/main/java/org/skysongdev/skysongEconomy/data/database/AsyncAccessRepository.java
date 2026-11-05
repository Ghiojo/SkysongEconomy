package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.AccessRepository;

import java.util.List;
import java.util.Map;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}; share the businesses' writer
 * so a business row is always saved before its groups. Loading stays on the calling thread.
 */
public final class AsyncAccessRepository implements AccessRepository {
    private final AccessRepository delegate;
    private final BackgroundWriter writer;

    public AsyncAccessRepository(AccessRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public Map<String, List<AccessGroup>> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(String businessId, List<AccessGroup> groups) {
        writer.run("save the access groups of business " + businessId, () -> delegate.save(businessId, groups));
    }
}
