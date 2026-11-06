package org.skysongdev.skysongEconomy.nodes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps nodes in a map, so tests can see what was saved. */
final class InMemoryNodeRepository implements NodeRepository {
    final Map<String, NodeSnapshot> saved = new LinkedHashMap<>();

    @Override
    public List<NodeSnapshot> loadAll() {
        return new ArrayList<>(saved.values());
    }

    @Override
    public void save(NodeSnapshot node) {
        saved.put(node.id(), node);
    }

    @Override
    public void delete(String id) {
        saved.remove(id);
    }
}
