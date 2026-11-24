package org.skysongdev.skysongEconomy.nodes;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.nodes.NodeInfo;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeInfosTest {
    @Test
    void copiesAPublicRandomNode() {
        Node node = Node.create("north_mine", NodeKind.RANDOM, "iron_vein", null);

        assertEquals(new NodeInfo("north_mine", NodeInfo.RANDOM, "iron_vein", 1, Optional.empty(), Optional.empty()),
                NodeInfos.of(node));
    }

    @Test
    void copiesABusinessFarmWithItsName() {
        Node node = Node.restore(new NodeSnapshot("rose_farm", NodeKind.FARM, "wheat", 2,
                new NodeOwner.Business("rose_smithy"), "Rose farm", null, true));

        assertEquals(new NodeInfo("rose_farm", NodeInfo.FARM, "wheat", 2, Optional.of("rose_smithy"),
                Optional.of("Rose farm")), NodeInfos.of(node));
    }
}
