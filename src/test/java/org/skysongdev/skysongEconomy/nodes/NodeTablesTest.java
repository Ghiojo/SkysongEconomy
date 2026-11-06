package org.skysongdev.skysongEconomy.nodes;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.FileRegistry;
import org.skysongdev.skysongEconomy.loot.LootEntry;
import org.skysongdev.skysongEconomy.loot.LootOutput;
import org.skysongdev.skysongEconomy.nodes.farm.FarmResource;
import org.skysongdev.skysongEconomy.nodes.farm.FarmTier;
import org.skysongdev.skysongEconomy.nodes.farm.FarmType;
import org.skysongdev.skysongEconomy.nodes.random.RandomTable;
import org.skysongdev.skysongEconomy.nodes.random.RandomTier;
import org.skysongdev.skysongEconomy.trade.IntRange;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeTablesTest {
    private static final LootOutput COAL = new LootOutput(List.of(new LootEntry.Item("coal", IntRange.exactly(1))),
            List.of(), List.of());

    private static RandomTable table(String id, String name) {
        return new RandomTable(id, name, id, "mining", Map.of(1, new RandomTier(1, 1, Optional.empty(), COAL)));
    }

    private static FarmResource resource(String id) {
        return new FarmResource(id, id, new ItemAmount(id + "_seeds", 1), Map.of(1, new FarmTier(1, 1, COAL)));
    }

    private static NodeTables tables(List<RandomTable> random, List<FarmType> types, List<FarmResource> resources) {
        return new NodeTables(new FileRegistry<>(random), new FileRegistry<>(types), new FileRegistry<>(resources));
    }

    @Test
    void namesTheRandomTablesAndFarmTypesThatLoaded() {
        NodeTables tables = tables(List.of(table("mine", "<gray>Mine")),
                List.of(new FarmType("crop", "<green>Crop plot", "farming", List.of("wheat"))), List.of(resource("wheat")));

        assertEquals(Optional.of("<gray>Mine"), tables.name(NodeKind.RANDOM, "mine"));
        assertEquals(Optional.empty(), tables.name(NodeKind.RANDOM, "forest"));
        assertEquals(Optional.of("<green>Crop plot"), tables.name(NodeKind.FARM, "crop"));
        assertEquals(Optional.empty(), tables.name(NodeKind.FARM, "mine"), "a random table isn't a farm type");
    }

    @Test
    void replacingSwapsTheTablesNodesSee() {
        NodeTables tables = tables(List.of(), List.of(), List.of());
        Node node = Node.create("quarry", NodeKind.RANDOM, "mine", null);
        assertTrue(tables.randomTable(node).isEmpty());

        tables.replace(new FileRegistry<>(List.of(table("mine", "Mine"))), FileRegistry.empty(), FileRegistry.empty());

        assertEquals("Mine", tables.randomTable(node).orElseThrow().name());
        assertTrue(tables.randomTable(Node.create("field", NodeKind.FARM, "mine", null)).isEmpty(),
                "a farm node never uses a random table");
    }

    @Test
    void aFarmNodeUsesItsTypeAndTheTypesResourcesThatLoaded() {
        FarmType crop = new FarmType("crop", "Crop plot", "farming", List.of("wheat", "barley"));
        NodeTables tables = tables(List.of(table("crop", "Not a farm")), List.of(crop),
                List.of(resource("barley"), resource("wheat")));
        Node field = Node.create("field", NodeKind.FARM, "crop", null);

        assertEquals(Optional.of(crop), tables.farmType(field));
        assertTrue(tables.farmType(Node.create("quarry", NodeKind.RANDOM, "crop", null)).isEmpty(),
                "a random node never uses a farm type");
        assertEquals(List.of("wheat", "barley"), tables.resourcesOf(crop).stream().map(FarmResource::id).toList(),
                "in the type file's order");
    }

    @Test
    void aResourceThatIsGoneAfterAReloadIsNoLongerOffered() {
        FarmType crop = new FarmType("crop", "Crop plot", "farming", List.of("wheat", "barley"));
        NodeTables tables = tables(List.of(), List.of(crop), List.of(resource("wheat"), resource("barley")));

        tables.replace(FileRegistry.empty(), new FileRegistry<>(List.of(crop)), new FileRegistry<>(List.of(resource("wheat"))));

        assertEquals(List.of("wheat"), tables.resourcesOf(crop).stream().map(FarmResource::id).toList());
    }
}
