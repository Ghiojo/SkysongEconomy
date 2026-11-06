package org.skysongdev.skysongEconomy.nodes.farm;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.FileRegistry;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.nodes.harvest.Harvest;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FarmHarvesterTest {
    private final FarmWorld world = new FarmWorld();
    private final FarmHarvester harvester = world.harvester;

    private String refusal(Runnable action) {
        return assertThrows(RefusedException.class, action::run).messageKey();
    }

    @Test
    void workersPayTheTiersTokensAndGetItsOutput() {
        world.plantWheat();
        world.tokens.add(world.worker, 5);
        world.farmingLevels.put(world.worker, "novice");

        Harvest harvest = harvester.harvest(world.field, world.worker, 2);

        assertEquals(List.of(new ItemAmount("wheat", 5)), harvest.items());
        assertEquals(HarvestCost.tokens(3), harvest.paid());
        assertEquals(2, world.tokens.balance(world.worker));
        assertEquals(List.of(harvest), world.heard);
    }

    @Test
    void offersTheResourcesListedTiersUpToTheNodesTier() {
        assertEquals(List.of(), harvester.offered(world.field), "nothing is planted");
        world.plantWheat();
        assertEquals(List.of(1, 2), harvester.offered(world.field).stream().map(FarmTier::tier).toList());

        world.downtime.put(world.owner.player(), 1);
        world.plots.takeOut(world.field, world.owner);
        world.plots.plant(world.field, world.owner, world.resource("barley"), FarmWorld.SEEDS);

        assertEquals(List.of(), harvester.offered(world.field), "barley starts at tier 3 and the node is tier 2");
        assertEquals("node.harvest.no-tier", refusal(() -> harvester.harvest(world.field, world.owner, 3)));
        world.nodes.setTier(world.field, 3);
        assertEquals(List.of(3), harvester.offered(world.field).stream().map(FarmTier::tier).toList());
    }

    @Test
    void refusesStrangersLowLevelsMissingTokensAndEmptyPlots() {
        assertEquals("node.farm.empty", refusal(() -> harvester.harvest(world.field, world.owner, 1)));
        world.plantWheat();

        assertEquals("node.harvest.not-member", refusal(() -> harvester.harvest(world.field, world.stranger, 1)));
        assertEquals("node.harvest.low-level", refusal(() -> harvester.harvest(world.field, world.owner, 2)));
        assertEquals("node.harvest.no-tokens", refusal(() -> harvester.harvest(world.field, world.owner, 1)));
        assertEquals(List.of(), world.heard, "nothing was harvested");
    }

    @Test
    void aResourceWhoseFileIsGoneCantBeHarvestedButStaysPlanted() {
        world.plantWheat();
        world.tables.replace(FileRegistry.empty(), world.tables.farmTypes(), FileRegistry.empty());

        assertEquals("node.farm.resource-unavailable", refusal(() -> harvester.harvest(world.field, world.owner, 1)));
        assertEquals(List.of(), harvester.offered(world.field));
        assertEquals("wheat", world.plots.planting(world.field).orElseThrow().resourceId());
        assertEquals("wheat", world.plots.plantedName(world.field).orElseThrow(), "shown by its id");
    }
}
