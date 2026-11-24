package org.skysongdev.skysongEconomy.api.nodes;

import java.util.Objects;
import java.util.Optional;

/**
 * A gathering node as other plugins see it, at the moment it was read.
 *
 * @param id              unique, such as {@code north_mine}
 * @param kind            {@code random} (mines, forestry…) or {@code farm}
 * @param source          its random table or farm type id
 * @param tier            its tier, from 1
 * @param owningBusiness  the business it belongs to; empty for public or unowned nodes
 * @param displayName     the name players see, when it has one
 */
public record NodeInfo(String id, String kind, String source, int tier, Optional<String> owningBusiness,
                       Optional<String> displayName) {
    /** {@link #kind()} of a node that rolls on a random table. */
    public static final String RANDOM = "random";
    /** {@link #kind()} of a farm node. */
    public static final String FARM = "farm";

    public NodeInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(owningBusiness, "owningBusiness");
        Objects.requireNonNull(displayName, "displayName");
    }
}
