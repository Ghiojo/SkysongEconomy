package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcRepository;

import java.util.List;
import java.util.UUID;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}, so creating or removing an NPC
 * never waits for the database. Loading stays on the calling thread.
 */
public final class AsyncBusinessNpcRepository implements BusinessNpcRepository {
    private final BusinessNpcRepository delegate;
    private final BackgroundWriter writer;

    public AsyncBusinessNpcRepository(BusinessNpcRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<BusinessNpc> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(BusinessNpc npc) {
        writer.run("save the business NPC " + npc.npc(), () -> delegate.save(npc));
    }

    @Override
    public void delete(UUID npc) {
        writer.run("delete the business NPC " + npc, () -> delegate.delete(npc));
    }
}
