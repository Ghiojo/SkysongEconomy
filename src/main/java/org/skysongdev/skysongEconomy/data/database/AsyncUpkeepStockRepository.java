package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.items.StoredItems;
import org.skysongdev.skysongEconomy.upkeep.UpkeepStockRepository;

import java.util.Map;
import java.util.UUID;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}, so stocking or paying from an
 * NPC never waits for the database. Loading stays on the calling thread.
 */
public final class AsyncUpkeepStockRepository implements UpkeepStockRepository {
    private final UpkeepStockRepository delegate;
    private final BackgroundWriter writer;

    public AsyncUpkeepStockRepository(UpkeepStockRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public Map<UUID, StoredItems> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(UUID npc, StoredItems items) {
        writer.run("save the upkeep stock of the NPC " + npc, () -> delegate.save(npc, items));
    }

    @Override
    public void delete(UUID npc) {
        writer.run("delete the upkeep stock of the NPC " + npc, () -> delegate.delete(npc));
    }
}
