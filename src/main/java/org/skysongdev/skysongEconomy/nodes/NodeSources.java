package org.skysongdev.skysongEconomy.nodes;

import java.util.Optional;

/**
 * The random tables and farm types nodes can use, as loaded from their folders. The node core
 * only needs to know which exist and what they are called; see {@link NodeTables}.
 */
@FunctionalInterface
public interface NodeSources {
    /** @return the display name, in MiniMessage, of that random table or farm type; empty when there is none */
    Optional<String> name(NodeKind kind, String id);
}
