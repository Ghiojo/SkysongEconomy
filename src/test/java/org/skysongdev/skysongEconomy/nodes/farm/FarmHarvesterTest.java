package org.skysongdev.skysongEconomy.nodes.farm;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.FileRegistry;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.nodes.boost.Booster;
import org.skysongdev.skysongEconomy.loot.YieldEffect;
import org.skysongdev.skysongEconomy.nodes.harvest.Harvest;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;
import org.skysongdev.skysongEconomy.nodes.modifiers.CalendarModifier;
import org.skysongdev.skysongEconomy.nodes.modifiers.CalendarWindow;
import org.skysongdev.skysongEconomy.nodes.modifiers.NodeTarget;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(Optional.empty(), harvest.booster());
        assertEquals(Optional.of("wheat"), harvest.resource());
    }

    @Test
    void aBoosterForTheFarmTypeChangesTheAmountsAndOthersAreRefused() {
        world.plantWheat();
        world.tokens.add(world.worker, 5);
        world.farmingLevels.put(world.worker, "novice");

        assertEquals("crop", harvester.boosterType(world.field));
        assertEquals("node.booster.wrong-type", refusal(() -> harvester.harvest(world.field, world.worker, 2,
                Optional.of(new Booster("pen", YieldEffect.parse("max+1"))))));
        assertEquals(5, world.tokens.balance(world.worker), "nothing was charged");

        Harvest harvest = harvester.harvest(world.field, world.worker, 2,
                Optional.of(new Booster("crop", YieldEffect.parse("min-2; max-2"))));
        assertEquals(List.of(new ItemAmount("wheat", 3)), harvest.items());
        assertEquals("crop", harvest.booster().orElseThrow().type());
        assertEquals(Optional.of("wheat"), harvest.resource());
    }

    private static CalendarModifier winterOn(String resource, String effect) {
        boolean dormant = effect.equals("dormant");
        return new CalendarModifier("winter-" + resource, "Winter", new CalendarWindow(Set.of("vinse_er"), List.of(), false),
                new NodeTarget(Set.of(), Set.of(), Set.of(), Set.of(resource), Set.of(), Set.of()),
                dormant ? YieldEffect.NONE : YieldEffect.parse(effect), dormant);
    }

    @Test
    void calendarModifiersFollowWhatIsPlanted() {
        world.plantWheat();
        world.tokens.add(world.worker, 10);
        world.farmingLevels.put(world.worker, "novice");
        world.modifierFiles = new FileRegistry<>(List.of(winterOn("wheat", "max-60%"), winterOn("barley", "dormant")));

        Harvest harvest = harvester.harvest(world.field, world.worker, 2);

        assertEquals(List.of(new ItemAmount("wheat", 2)), harvest.items(), "5 - 60% = 2");
        assertEquals(List.of("winter-wheat"), harvest.modifiers(), "barley's modifier doesn't hit wheat");
    }

    @Test
    void anEmptyFarmHasNoModifiersOnItsNameplate() {
        world.modifierFiles = new FileRegistry<>(List.of(winterOn("wheat", "max-1")));

        assertTrue(harvester.modifiersOnNode(world.field).isEmpty());
        world.plantWheat();
        assertEquals(List.of("winter-wheat"), harvester.modifiersOnNode(world.field).ids());
    }

    @Test
    void aDormantModifierStopsFarmHarvestsAndChargesNothing() {
        world.plantWheat();
        world.tokens.add(world.worker, 5);
        world.farmingLevels.put(world.worker, "novice");
        world.modifierFiles = new FileRegistry<>(List.of(winterOn("wheat", "dormant")));

        assertEquals("node.harvest.dormant", refusal(() -> harvester.harvest(world.field, world.worker, 2)));
        assertEquals(5, world.tokens.balance(world.worker));
        assertEquals(List.of(), world.heard);
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
