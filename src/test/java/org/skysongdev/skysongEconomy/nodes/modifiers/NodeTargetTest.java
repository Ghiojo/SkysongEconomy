package org.skysongdev.skysongEconomy.nodes.modifiers;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeTargetTest {
    private static final NodeFacts HERB_PATCH = NodeFacts.random("herb_patch", "herbs", "foraging");
    private static final NodeFacts QUARRY = NodeFacts.random("quarry", "mine", "mining");
    private static final NodeFacts FIELD = NodeFacts.farm("field", "crop", "wheat");

    @Test
    void matchesRandomNodesByTableOrType() {
        NodeTarget byTable = new NodeTarget(Set.of("herbs"), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
        NodeTarget byType = new NodeTarget(Set.of(), Set.of("foraging"), Set.of(), Set.of(), Set.of(), Set.of());

        assertTrue(byTable.matches(HERB_PATCH, 1));
        assertTrue(byType.matches(HERB_PATCH, 1));
        assertFalse(byTable.matches(QUARRY, 1));
        assertFalse(byType.matches(FIELD, 1));
    }

    @Test
    void matchesFarmNodesByTypeOrWhatIsPlanted() {
        NodeTarget byType = new NodeTarget(Set.of(), Set.of(), Set.of("crop"), Set.of(), Set.of(), Set.of());
        NodeTarget byResource = new NodeTarget(Set.of(), Set.of(), Set.of(), Set.of("wheat"), Set.of(), Set.of());

        assertTrue(byType.matches(FIELD, 1));
        assertTrue(byResource.matches(FIELD, 1));
        assertFalse(byResource.matches(NodeFacts.farm("field", "crop", "carrots"), 1));
        assertFalse(byType.matches(HERB_PATCH, 1), "a random table named crop is not a farm type");
    }

    @Test
    void matchesSingleNodesOfAnyKind() {
        NodeTarget nodes = new NodeTarget(Set.of(), Set.of(), Set.of(), Set.of(), Set.of("quarry", "field"), Set.of());

        assertTrue(nodes.matches(QUARRY, 3));
        assertTrue(nodes.matches(FIELD, 3));
        assertFalse(nodes.matches(HERB_PATCH, 3));
    }

    @Test
    void listedTiersLimitEveryLine() {
        NodeTarget lowTiers = new NodeTarget(Set.of(), Set.of(), Set.of("crop"), Set.of(), Set.of(), Set.of(1, 2));

        assertTrue(lowTiers.matches(FIELD, 2));
        assertFalse(lowTiers.matches(FIELD, 3));
    }

    @Test
    void needsSomethingToHit() {
        assertThrows(IllegalArgumentException.class,
                () -> new NodeTarget(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(1)));
    }
}
