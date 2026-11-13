package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.npc.benefit.NpcApplied;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcAppliedRepository;

import java.util.Map;
import java.util.UUID;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}; share the business NPCs'
 * writer so an NPC's row is always saved before what was changed on its sheet. Loading stays on
 * the calling thread.
 */
public final class AsyncNpcAppliedRepository implements NpcAppliedRepository {
    private final NpcAppliedRepository delegate;
    private final BackgroundWriter writer;

    public AsyncNpcAppliedRepository(NpcAppliedRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public Map<UUID, NpcApplied> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(UUID npc, NpcApplied applied) {
        writer.run("save the sheet changes of the business NPC " + npc, () -> delegate.save(npc, applied));
    }
}
