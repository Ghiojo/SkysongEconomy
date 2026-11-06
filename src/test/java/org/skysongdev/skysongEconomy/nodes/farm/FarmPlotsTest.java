package org.skysongdev.skysongEconomy.nodes.farm;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeKind;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmPlotsTest {
    private final FarmWorld world = new FarmWorld();
    private final FarmPlots plots = world.plots;

    private String refusal(Runnable action) {
        return assertThrows(RefusedException.class, action::run).messageKey();
    }

    @Test
    void theOwnerPlantsForFreeAndItIsSaved() {
        Planting planting = world.plantWheat();

        assertEquals(new Planting("field", "wheat", "crop", "farmers", world.owner, FarmWorld.SEEDS), planting);
        assertEquals(Optional.of(planting), plots.planting(world.field));
        assertEquals(Optional.of("<green>wheat"), plots.plantedName(world.field));
        assertEquals(Map.of("field", planting), world.savedPlantings);
        assertEquals(List.of("field"), world.plantingChanges, "the nameplate is told");
    }

    @Test
    void workersTendOnlyWithTheTendFarmsPermission() {
        assertFalse(plots.canTend(world.field, world.worker));
        assertEquals("node.farm.no-tending",
                refusal(() -> plots.plant(world.field, world.worker, world.resource("wheat"), FarmWorld.SEEDS)));
        assertEquals("node.farm.no-tending", refusal(() -> plots.takeOut(world.field, world.stranger)));

        world.letWorkersTend();

        assertTrue(plots.canTend(world.field, world.worker));
        assertFalse(plots.canTend(world.field, world.stranger));
        plots.plant(world.field, world.worker, world.resource("wheat"), FarmWorld.SEEDS);
        assertEquals(world.worker, plots.planting(world.field).orElseThrow().planter());
    }

    @Test
    void refusesAFullPlotAndResourcesOfOtherTypes() {
        assertEquals("node.farm.wrong-primer",
                refusal(() -> plots.plant(world.field, world.owner, world.resource("sheep"), FarmWorld.SHEEP)));
        world.plantWheat();

        assertEquals("node.farm.occupied", refusal(world::plantWheat));
    }

    @Test
    void theTagWinsAndUntaggedItemsMatchTheFirstPrimerItem() {
        assertEquals("barley", plots.resourceFor(world.field, Optional.of("barley"), Optional.of("wheat_seeds")).id());
        assertEquals("wheat", plots.resourceFor(world.field, Optional.empty(), Optional.of("wheat_seeds")).id());
        assertEquals("node.farm.not-a-primer",
                refusal(() -> plots.resourceFor(world.field, Optional.empty(), Optional.of("sheep_token"))));
        assertEquals("node.farm.not-a-primer", refusal(() -> plots.resourceFor(world.field, Optional.empty(), Optional.empty())));
        assertEquals("node.farm.wrong-primer",
                refusal(() -> plots.resourceFor(world.field, Optional.of("sheep"), Optional.empty())));
        assertEquals("node.farm.unknown-primer",
                refusal(() -> plots.resourceFor(world.field, Optional.of("cactus"), Optional.empty())));
    }

    @Test
    void takingOutCostsDowntimeAndGivesTheExactItemsBack() {
        world.plantWheat();
        assertEquals("node.farm.no-downtime", refusal(() -> plots.takeOut(world.field, world.owner)));
        assertTrue(plots.planting(world.field).isPresent(), "nothing changes when it can't be paid");

        world.downtime.put(world.owner.player(), 3);

        assertEquals(FarmWorld.SEEDS, plots.takeOut(world.field, world.owner));
        assertEquals(2, world.downtime.get(world.owner.player()));
        assertTrue(plots.planting(world.field).isEmpty());
        assertEquals(Map.of(), world.savedPlantings);
        assertEquals("node.farm.empty", refusal(() -> plots.takeOut(world.field, world.owner)));
    }

    @Test
    void aNodeWithoutABusinessCantBeTended() {
        Node loose = world.nodes.create("loose", NodeKind.FARM, "crop", new BlockPosition("world", 9, 64, 9));

        assertEquals("node.harvest.unowned", refusal(() -> plots.plant(loose, world.owner, world.resource("wheat"), FarmWorld.SEEDS)));
    }

    @Test
    void removingTheNodeGivesThePrimerToTheOnlinePlanter() {
        world.plantWheat();
        world.online.add(world.owner.player());

        world.nodes.delete(world.field);

        assertEquals(Map.of(world.owner.player(), List.of(FarmWorld.SEEDS)), world.received);
        assertEquals(Map.of(), world.savedPlantings);
    }

    @Test
    void anOfflinePlanterGetsThePrimerWhenTheyJoin() {
        world.plantWheat();

        world.businesses.delete(world.farmers);

        assertTrue(plots.planting(world.field).isEmpty(), "a deleted business's plots are emptied");
        assertEquals(1, world.savedReturns.size(), "kept until they are online");
        assertEquals(Map.of(), world.received);

        world.online.add(world.owner.player());
        assertEquals(1, world.returns.deliverWaiting(world.owner.player()));
        assertEquals(Map.of(world.owner.player(), List.of(FarmWorld.SEEDS)), world.received);
        assertEquals(Map.of(), world.savedReturns);
    }

    @Test
    void changingTheOwnerOrTypeGivesThePrimerBackButChangingTheTierDoesNot() {
        world.plantWheat();
        world.nodes.setTier(world.field, 3);
        assertTrue(plots.planting(world.field).isPresent());

        world.nodes.setSource(world.field, "pen");

        assertTrue(plots.planting(world.field).isEmpty());
        assertEquals(1, world.savedReturns.size());
    }

    @Test
    void plantingsWhoseNodeIsGoneOrChangedAreGivenBackOnLoad() {
        world.savedPlantings.put("gone", new Planting("gone", "wheat", "crop", "farmers", world.worker, FarmWorld.SEEDS));
        world.savedPlantings.put("field", new Planting("field", "wheat", "crop", "farmers", world.owner, FarmWorld.SEEDS));
        assertEquals(2, plots.loadAll());

        plots.loaded(world.nodes.all());

        assertEquals(List.of("field"), List.copyOf(world.savedPlantings.keySet()));
        assertEquals(world.worker.player(), world.savedReturns.values().iterator().next().player());
    }
}
