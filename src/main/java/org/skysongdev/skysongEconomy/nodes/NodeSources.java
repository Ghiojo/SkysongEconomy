package org.skysongdev.skysongEconomy.nodes;

import java.util.Optional;

/**
 * The random tables and farm types nodes can use, as loaded from their folders. The node core
 * only needs to know which exist and what they are called.
 */
@FunctionalInterface
public interface NodeSources {
    /**
     * Accepts every id and names it after itself. Used until the random tables and farm types
     * are loaded from their folders.
     */
    NodeSources ANY = (kind, id) -> Optional.of(id);

    /** @return the display name of that random table or farm type, empty when there is none */
    Optional<String> name(NodeKind kind, String id);
}
