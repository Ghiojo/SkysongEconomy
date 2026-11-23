package org.skysongdev.skysongEconomy.nodes.modifiers;

import org.skysongdev.skysongEconomy.nodes.NodeKind;

import java.util.Objects;
import java.util.Optional;

/**
 * What a modifier's {@code applies-to} can match on one node at harvest time.
 *
 * @param nodeId     the node's id
 * @param kind       random or farm
 * @param source     the random table's or farm type's id
 * @param randomType the random table's booster {@code type}; empty on a farm node
 * @param resource   what is planted in a farm node; empty on a random node or an empty farm
 */
public record NodeFacts(String nodeId, NodeKind kind, String source, Optional<String> randomType,
                        Optional<String> resource) {
    public NodeFacts {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(randomType, "randomType");
        Objects.requireNonNull(resource, "resource");
    }

    /** @return a random node using that table, whose booster type is {@code type} */
    public static NodeFacts random(String nodeId, String table, String type) {
        return new NodeFacts(nodeId, NodeKind.RANDOM, table, Optional.of(type), Optional.empty());
    }

    /** @return a farm node of that farm type with {@code resource} planted */
    public static NodeFacts farm(String nodeId, String farmType, String resource) {
        return new NodeFacts(nodeId, NodeKind.FARM, farmType, Optional.empty(), Optional.of(resource));
    }
}
