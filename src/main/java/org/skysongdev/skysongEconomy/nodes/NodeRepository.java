package org.skysongdev.skysongEconomy.nodes;

import java.util.List;

/** Where nodes are saved. */
public interface NodeRepository {
    /** @return every saved node */
    List<NodeSnapshot> loadAll();

    /** Saves the node, replacing any earlier copy with the same id. */
    void save(NodeSnapshot node);

    /** Deletes the node with that id, if there is one. */
    void delete(String id);
}
