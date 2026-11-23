package org.skysongdev.skysongEconomy.nodes.modifiers;

import org.skysongdev.skysongEconomy.nodes.NodeKind;

import java.util.Set;

/**
 * Which nodes a calendar modifier hits, from its {@code applies-to} section: a node matches when
 * any line names it, and then only at the listed tiers, if any are listed. Ids are lower-case.
 *
 * @param randomTables  random table ids
 * @param randomTypes   random tables' booster types, such as {@code foraging}
 * @param farmTypes     farm type ids
 * @param farmResources farm resource ids, matching what is planted
 * @param nodes         node ids
 * @param tiers         the tiers it applies at; empty for every tier
 */
public record NodeTarget(Set<String> randomTables, Set<String> randomTypes, Set<String> farmTypes,
                         Set<String> farmResources, Set<String> nodes, Set<Integer> tiers) {
    public NodeTarget {
        randomTables = Set.copyOf(randomTables);
        randomTypes = Set.copyOf(randomTypes);
        farmTypes = Set.copyOf(farmTypes);
        farmResources = Set.copyOf(farmResources);
        nodes = Set.copyOf(nodes);
        tiers = Set.copyOf(tiers);
        if (randomTables.isEmpty() && randomTypes.isEmpty() && farmTypes.isEmpty() && farmResources.isEmpty()
                && nodes.isEmpty()) {
            throw new IllegalArgumentException("A target needs at least one table, type, resource or node");
        }
    }

    /** @return whether a harvest at {@code tier} on that node is hit */
    public boolean matches(NodeFacts node, int tier) {
        if (!tiers.isEmpty() && !tiers.contains(tier)) {
            return false;
        }
        if (nodes.contains(node.nodeId())) {
            return true;
        }
        if (node.kind() == NodeKind.RANDOM) {
            return randomTables.contains(node.source()) || node.randomType().filter(randomTypes::contains).isPresent();
        }
        return farmTypes.contains(node.source()) || node.resource().filter(farmResources::contains).isPresent();
    }
}
