package org.skysongdev.skysongEconomy.nodes;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BlockPosition;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeTest {
    private static final BlockPosition BLOCK = new BlockPosition("world", 1, 64, 1);

    @Test
    void aNewNodeIsTierOneAndPublicWithNoNameOfItsOwn() {
        Node node = Node.create("quarry", NodeKind.RANDOM, "mine", BLOCK);

        assertEquals(1, node.tier());
        assertEquals(NodeOwner.PUBLIC, node.owner());
        assertTrue(node.displayName().isEmpty());
        assertTrue(node.owningBusiness().isEmpty());
        assertEquals(Optional.of(BLOCK), node.position());
    }

    @Test
    void aSnapshotRestoresTheSameNode() {
        NodeSnapshot snapshot = new NodeSnapshot("field", NodeKind.FARM, "crop", 4,
                new NodeOwner.Business("farmers"), "North field", null);

        Node node = Node.restore(snapshot);

        assertEquals(snapshot, node.snapshot());
        assertEquals(Optional.of("farmers"), node.owningBusiness());
    }

    @Test
    void tiersOutsideOneToFiveAreRefused() {
        Node node = Node.create("quarry", NodeKind.RANDOM, "mine", BLOCK);

        assertThrows(IllegalArgumentException.class, () -> node.setTier(6));
        assertThrows(IllegalArgumentException.class, () -> Node.restore(
                new NodeSnapshot("pit", NodeKind.RANDOM, "mine", 0, NodeOwner.PUBLIC, null, null)));
    }

    @Test
    void kindsAreReadFromTheirKeysInAnyCase() {
        assertEquals(Optional.of(NodeKind.RANDOM), NodeKind.fromKey("Random"));
        assertEquals(Optional.of(NodeKind.FARM), NodeKind.fromKey("farm"));
        assertEquals(Optional.empty(), NodeKind.fromKey("mine"));
        assertEquals("farm", NodeKind.FARM.key());
    }
}
