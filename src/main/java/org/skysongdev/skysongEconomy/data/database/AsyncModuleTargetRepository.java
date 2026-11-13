package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.module.npc.ModuleTargetRepository;

import java.util.List;
import java.util.UUID;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}, so choosing an NPC for a
 * module never waits for the database. Loading stays on the calling thread.
 */
public final class AsyncModuleTargetRepository implements ModuleTargetRepository {
    private final ModuleTargetRepository delegate;
    private final BackgroundWriter writer;

    public AsyncModuleTargetRepository(ModuleTargetRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<Row> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(Row row) {
        writer.run("save the NPC " + row.npc() + " chosen for a module of " + row.businessId(), () -> delegate.save(row));
    }

    @Override
    public void deletePick(String businessId, int pickOrder) {
        writer.run("delete the choices of module pick " + pickOrder + " of " + businessId,
                () -> delegate.deletePick(businessId, pickOrder));
    }

    @Override
    public void deleteNpc(UUID npc) {
        writer.run("delete the module choices of the NPC " + npc, () -> delegate.deleteNpc(npc));
    }
}
