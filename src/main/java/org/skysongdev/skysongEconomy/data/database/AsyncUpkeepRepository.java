package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepRepository;

import java.util.List;
import java.util.UUID;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}, so settling upkeep cycles never
 * waits for the database. Loading stays on the calling thread.
 */
public final class AsyncUpkeepRepository implements UpkeepRepository {
    private final UpkeepRepository delegate;
    private final BackgroundWriter writer;

    public AsyncUpkeepRepository(UpkeepRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<NpcUpkeep> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(NpcUpkeep upkeep) {
        writer.run("save the upkeep of the NPC " + upkeep.npc(), () -> delegate.save(upkeep));
    }

    @Override
    public void delete(UUID npc) {
        writer.run("delete the upkeep of the NPC " + npc, () -> delegate.delete(npc));
    }
}
