package org.skysongdev.skysongEconomy.nodes;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.loot.LootEntry;
import org.skysongdev.skysongEconomy.loot.LootOutput;
import org.skysongdev.skysongEconomy.nodes.random.RandomTable;
import org.skysongdev.skysongEconomy.nodes.random.RandomTableRegistry;
import org.skysongdev.skysongEconomy.nodes.random.RandomTier;
import org.skysongdev.skysongEconomy.trade.IntRange;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeTablesTest {
    private static RandomTable table(String id, String name) {
        LootOutput output = new LootOutput(List.of(new LootEntry.Item("coal", IntRange.exactly(1))), List.of(), List.of());
        return new RandomTable(id, name, id, "mining", Map.of(1, new RandomTier(1, 1, Optional.empty(), output)));
    }

    @Test
    void namesRandomTablesThatLoadedAndAnyFarmTypeForNow() {
        NodeTables tables = new NodeTables(new RandomTableRegistry(List.of(table("mine", "<gray>Mine"))));

        assertEquals(Optional.of("<gray>Mine"), tables.name(NodeKind.RANDOM, "mine"));
        assertEquals(Optional.empty(), tables.name(NodeKind.RANDOM, "forest"));
        assertEquals(Optional.of("crop"), tables.name(NodeKind.FARM, "crop"));
    }

    @Test
    void replacingSwapsTheTablesNodesSee() {
        NodeTables tables = new NodeTables(RandomTableRegistry.empty());
        Node node = Node.create("quarry", NodeKind.RANDOM, "mine", null);
        assertTrue(tables.randomTable(node).isEmpty());

        tables.replace(new RandomTableRegistry(List.of(table("mine", "Mine"))));

        assertEquals("Mine", tables.randomTable(node).orElseThrow().name());
        assertTrue(tables.randomTable(Node.create("field", NodeKind.FARM, "mine", null)).isEmpty(),
                "a farm node never uses a random table");
    }
}
