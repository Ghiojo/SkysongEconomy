package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.nodes.farm.Planting;
import org.skysongdev.skysongEconomy.nodes.farm.PlantingRepository;

import java.util.List;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}. Loading stays on the calling
 * thread because the plugin needs the data before it can start.
 */
public final class AsyncPlantingRepository implements PlantingRepository {
    private final PlantingRepository delegate;
    private final BackgroundWriter writer;

    public AsyncPlantingRepository(PlantingRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<Planting> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(Planting planting) {
        writer.run("save the planting in node " + planting.nodeId(), () -> delegate.save(planting));
    }

    @Override
    public void delete(String nodeId) {
        writer.run("delete the planting in node " + nodeId, () -> delegate.delete(nodeId));
    }
}
