package org.skysongdev.skysongEconomy.nodes.harvest;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.SkillLevels;
import org.skysongdev.skysongEconomy.loot.LootRoller;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeException;
import org.skysongdev.skysongEconomy.nodes.NodeOwner;
import org.skysongdev.skysongEconomy.nodes.NodeSettings;
import org.skysongdev.skysongEconomy.nodes.NodeTables;
import org.skysongdev.skysongEconomy.nodes.boost.Booster;
import org.skysongdev.skysongEconomy.nodes.modifiers.ActiveModifiers;
import org.skysongdev.skysongEconomy.nodes.modifiers.NodeFacts;
import org.skysongdev.skysongEconomy.nodes.modifiers.NodeModifiers;
import org.skysongdev.skysongEconomy.nodes.random.RandomTable;
import org.skysongdev.skysongEconomy.nodes.random.RandomTier;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Gathers at random nodes. A harvest is instant: it checks who may gather, the skill level the
 * tier needs and the cost, takes the cost, rolls the tier's output and fires the harvest event.
 * Handing over the items is left to the caller.
 * <p>
 * Who may gather and what it costs depend on the owner: on a business-owned node, the owner
 * and workers pay the tier's {@code cost} in Harvest Tokens; on a public node, anyone pays its
 * {@code public-cost}; nobody gathers on an unowned node. Refusals throw a {@link NodeException}.
 * Paying is {@link HarvestPayer}'s job, which checks every part of the cost before taking any.
 * Not thread-safe: use it on the main thread.
 */
public final class RandomHarvester {
    /**
     * What gathering needs.
     *
     * @param tables    the loaded random tables
     * @param settings  the skill level each tier needs
     * @param access    who may gather where
     * @param payer     takes the cost
     * @param levels    characters' skill levels
     * @param events    told about each harvest
     * @param modifiers the calendar modifiers on today
     */
    public record Dependencies(NodeTables tables, NodeSettings settings, NodeAccess access, HarvestPayer payer,
                               SkillLevels levels, NodeHarvestEvents events, NodeModifiers modifiers) {
        public Dependencies {
            Objects.requireNonNull(tables, "tables");
            Objects.requireNonNull(settings, "settings");
            Objects.requireNonNull(access, "access");
            Objects.requireNonNull(payer, "payer");
            Objects.requireNonNull(levels, "levels");
            Objects.requireNonNull(events, "events");
            Objects.requireNonNull(modifiers, "modifiers");
        }
    }

    private final Dependencies deps;
    private final LootRoller roller;

    public RandomHarvester(Dependencies deps, LootRoller roller) {
        this.deps = Objects.requireNonNull(deps, "deps");
        this.roller = Objects.requireNonNull(roller, "roller");
    }

    /**
     * @return the table the node rolls on
     * @throws NodeException {@code node.harvest.unavailable} when it isn't a random node or its table didn't load
     */
    public RandomTable table(Node node) {
        return deps.tables().randomTable(node)
                .orElseThrow(() -> new NodeException("node.harvest.unavailable", Map.of("id", node.id())));
    }

    /** @return the tiers the node offers, lowest first: the table's tiers up to the node's tier */
    public List<RandomTier> offered(Node node) {
        return table(node).tiersUpTo(node.tier());
    }

    /** @return the skill level gathering at that tier needs */
    public String requiredLevel(RandomTier tier) {
        return deps.settings().levelFor(tier.tier());
    }

    /**
     * @return what one harvest at that tier costs on this node
     * @throws NodeException {@code node.harvest.business-only} on a public node when the tier has no public cost
     */
    public HarvestCost cost(Node node, RandomTier tier) {
        if (node.owner() instanceof NodeOwner.Public) {
            return tier.publicCost().orElseThrow(() -> new NodeException("node.harvest.business-only",
                    Map.of("tier", String.valueOf(tier.tier()))));
        }
        return HarvestCost.tokens(tier.cost());
    }

    /**
     * Checks that the character may gather here at all.
     *
     * @throws NodeException {@code node.harvest.unowned} on an unowned node, or
     *                       {@code node.harvest.not-member} when they don't work for the owning business
     */
    public void requireAccess(Node node, CharacterRef character) {
        deps.access().requireGatherer(node, character);
    }

    /**
     * @return what boosters must name to work here: the table's {@code type}
     * @throws NodeException {@code node.harvest.no-table} when the node's table isn't loaded
     */
    public String boosterType(Node node) {
        return table(node).type();
    }

    /**
     * @return the calendar modifiers on today for a harvest at that tier of the node
     * @throws NodeException {@code node.harvest.unavailable} when the node's table isn't loaded
     */
    public ActiveModifiers modifiers(Node node, RandomTier tier) {
        return deps.modifiers().activeFor(facts(node, table(node)), tier.tier());
    }

    /** @return the calendar modifiers on today for any tier the node offers; none when its table isn't loaded */
    public ActiveModifiers modifiersOnNode(Node node) {
        return deps.tables().randomTable(node)
                .map(table -> deps.modifiers().activeUpTo(facts(node, table), node.tier()))
                .orElse(ActiveModifiers.NONE);
    }

    private static NodeFacts facts(Node node, RandomTable table) {
        return NodeFacts.random(node.id(), table.id(), table.type()).withModifiers(node.modifiersEnabled());
    }

    /** Gathers once without a booster; see {@link #harvest(Node, CharacterRef, int, Optional)}. */
    public Harvest harvest(Node node, CharacterRef character, int tierNumber) {
        return harvest(node, character, tierNumber, Optional.empty());
    }

    /**
     * Gathers once: checks, pays, rolls and fires the harvest event. Using up the booster is
     * left to the caller, which holds the item.
     *
     * @param tierNumber the tier the player picked
     * @param booster    changes the amounts and rolls of this harvest
     * @return what was paid and rolled, for the caller to hand over
     * @throws NodeException when the node, the tier, the booster, the character's access, level or
     *                       money refuse it, or a calendar modifier on today is dormant
     *                       ({@code node.harvest.dormant}); nothing is charged then
     */
    public Harvest harvest(Node node, CharacterRef character, int tierNumber, Optional<Booster> booster) {
        RandomTable table = table(node);
        Booster.requireWorksOn(booster, table.type());
        RandomTier tier = table.tier(tierNumber).filter(found -> found.tier() <= node.tier())
                .orElseThrow(() -> new NodeException("node.harvest.no-tier", Map.of("tier", String.valueOf(tierNumber))));
        requireAccess(node, character);
        String level = requiredLevel(tier);
        if (!deps.levels().reaches(character, table.skill(), level)) {
            throw new NodeException("node.harvest.low-level", Map.of("level", level, "skill", table.skill()));
        }
        ActiveModifiers active = deps.modifiers().activeFor(facts(node, table), tier.tier());
        active.dormant().ifPresent(dormant -> {
            throw new NodeException("node.harvest.dormant", Map.of("modifier", dormant.name()));
        });
        HarvestCost cost = cost(node, tier);
        deps.payer().pay(character, cost);
        List<ItemAmount> items = roller.roll(tier.output(), active.effect().plus(Booster.effectOf(booster)));
        Harvest harvest = new Harvest(node, character, tier.tier(), cost, items, active.ids(), booster,
                Optional.empty());
        deps.events().harvested(harvest);
        return harvest;
    }
}
