package org.skysongdev.skysongEconomy.nodes.farm;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.SkillLevels;
import org.skysongdev.skysongEconomy.loot.LootRoller;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeException;
import org.skysongdev.skysongEconomy.nodes.NodeSettings;
import org.skysongdev.skysongEconomy.nodes.harvest.Harvest;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestPayer;
import org.skysongdev.skysongEconomy.nodes.harvest.NodeAccess;
import org.skysongdev.skysongEconomy.nodes.harvest.NodeHarvestEvents;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Harvests farm nodes. The owning business's owner and workers pick any tier the planted
 * resource lists, up to the node's tier; each tier needs its skill level in the farm type's
 * skill and costs its Harvest Tokens. There are no public farm nodes. Handing over the items is
 * left to the caller. Not thread-safe: use it on the main thread.
 */
public final class FarmHarvester {
    /**
     * What harvesting needs.
     *
     * @param plots    what is planted where
     * @param settings the skill level each tier needs
     * @param access   who works for the owning business
     * @param payer    takes the Harvest Tokens
     * @param levels   characters' skill levels
     * @param events   told about each harvest
     */
    public record Dependencies(FarmPlots plots, NodeSettings settings, NodeAccess access, HarvestPayer payer,
                               SkillLevels levels, NodeHarvestEvents events) {
        public Dependencies {
            Objects.requireNonNull(plots, "plots");
            Objects.requireNonNull(settings, "settings");
            Objects.requireNonNull(access, "access");
            Objects.requireNonNull(payer, "payer");
            Objects.requireNonNull(levels, "levels");
            Objects.requireNonNull(events, "events");
        }
    }

    private final Dependencies deps;
    private final LootRoller roller;

    public FarmHarvester(Dependencies deps, LootRoller roller) {
        this.deps = Objects.requireNonNull(deps, "deps");
        this.roller = Objects.requireNonNull(roller, "roller");
    }

    /**
     * @return the resource growing in the node, which the node's type still takes
     * @throws NodeException {@code node.farm.empty} when nothing is planted, or
     *                       {@code node.farm.resource-unavailable} when its file didn't load or the
     *                       type no longer lists it
     */
    public FarmResource growing(Node node) {
        Planting planting = deps.plots().planting(node).orElseThrow(() -> new NodeException("node.farm.empty"));
        FarmType type = deps.plots().type(node);
        return deps.plots().plantedResource(node).filter(resource -> type.accepts(resource.id()))
                .orElseThrow(() -> new NodeException("node.farm.resource-unavailable",
                        Map.of("resource", planting.resourceId())));
    }

    /** @return the tiers the node offers for what is growing, lowest first; none when nothing can be harvested */
    public List<FarmTier> offered(Node node) {
        try {
            return growing(node).tiersUpTo(node.tier());
        } catch (NodeException nothingGrowing) {
            return List.of();
        }
    }

    /** @return the skill level harvesting at that tier needs */
    public String requiredLevel(FarmTier tier) {
        return deps.settings().levelFor(tier.tier());
    }

    /**
     * Checks that the character may harvest here at all.
     *
     * @throws NodeException {@code node.harvest.unowned} or {@code node.harvest.not-member}
     */
    public void requireAccess(Node node, CharacterRef character) {
        deps.access().requireMember(node, character);
    }

    /**
     * Harvests once: checks, pays, rolls and fires the harvest event.
     *
     * @param tierNumber the tier the player picked
     * @return what was paid and rolled, for the caller to hand over
     * @throws NodeException when the node, what is planted, the tier, the character's access,
     *                       level or tokens refuse it; nothing is charged then
     */
    public Harvest harvest(Node node, CharacterRef character, int tierNumber) {
        FarmType type = deps.plots().type(node);
        requireAccess(node, character);
        FarmResource resource = growing(node);
        FarmTier tier = resource.tier(tierNumber).filter(found -> found.tier() <= node.tier())
                .orElseThrow(() -> new NodeException("node.harvest.no-tier", Map.of("tier", String.valueOf(tierNumber))));
        String level = requiredLevel(tier);
        if (!deps.levels().reaches(character, type.skill(), level)) {
            throw new NodeException("node.harvest.low-level", Map.of("level", level, "skill", type.skill()));
        }
        HarvestCost cost = HarvestCost.tokens(tier.cost());
        deps.payer().pay(character, cost);
        List<ItemAmount> items = roller.roll(tier.output());
        Harvest harvest = new Harvest(node, character, tier.tier(), cost, items);
        deps.events().harvested(harvest);
        return harvest;
    }
}
