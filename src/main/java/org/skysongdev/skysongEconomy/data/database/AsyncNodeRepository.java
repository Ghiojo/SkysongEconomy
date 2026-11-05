package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.nodes.NodeRepository;
import org.skysongdev.skysongEconomy.nodes.NodeSnapshot;

import java.util.List;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}. Loading stays on the calling
 * thread because the plugin needs the data before it can start.
 */
public final class AsyncNodeRepository implements NodeRepository {
    private final NodeRepository delegate;
    private final BackgroundWriter writer;

    public AsyncNodeRepository(NodeRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<NodeSnapshot> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(NodeSnapshot node) {
        writer.run("save node " + node.id(), () -> delegate.save(node));
    }

    @Override
    public void delete(String id) {
        writer.run("delete node " + id, () -> delegate.delete(id));
    }
}
