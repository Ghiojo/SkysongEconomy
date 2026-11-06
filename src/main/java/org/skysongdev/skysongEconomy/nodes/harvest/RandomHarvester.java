package org.skysongdev.skysongEconomy.nodes.harvest;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.DowntimeWallet;
import org.skysongdev.skysongEconomy.character.SkillLevels;
import org.skysongdev.skysongEconomy.loot.LootRoller;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeException;
import org.skysongdev.skysongEconomy.nodes.NodeOwner;
import org.skysongdev.skysongEconomy.nodes.NodeSettings;
import org.skysongdev.skysongEconomy.nodes.NodeTables;
import org.skysongdev.skysongEconomy.nodes.random.RandomTable;
import org.skysongdev.skysongEconomy.nodes.random.RandomTier;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Gathers at random nodes. A harvest is instant: it checks who may gather, the skill level the
 * tier needs and the cost, takes the cost, rolls the tier's output and fires the harvest event.
 * Handing over the items is left to the caller.
 * <p>
 * Who may gather and what it costs depend on the owner: on a business-owned node, the owner
 * and workers pay the tier's {@code cost} in Harvest Tokens; on a public node, anyone pays its
 * {@code public-cost}; nobody gathers on an unowned node. Refusals throw a {@link NodeException}.
 * Not thread-safe: use it on the main thread.
 */
public final class RandomHarvester {
    /**
     * What gathering needs.
     *
     * @param tables     the loaded random tables
     * @param settings   the skill level each tier needs
     * @param businesses the owners of business nodes
     * @param tokens     Harvest Tokens
     * @param downtime   downtime, for public costs
     * @param wallet     money, for public costs
     * @param levels     characters' skill levels
     * @param events     told about each harvest
     */
    public record Dependencies(NodeTables tables, NodeSettings settings, BusinessService businesses, GatheringTokens tokens,
                               DowntimeWallet downtime, Wallet wallet, SkillLevels levels, NodeHarvestEvents events) {
        public Dependencies {
            Objects.requireNonNull(tables, "tables");
            Objects.requireNonNull(settings, "settings");
            Objects.requireNonNull(businesses, "businesses");
            Objects.requireNonNull(tokens, "tokens");
            Objects.requireNonNull(downtime, "downtime");
            Objects.requireNonNull(wallet, "wallet");
            Objects.requireNonNull(levels, "levels");
            Objects.requireNonNull(events, "events");
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
        switch (node.owner()) {
            case NodeOwner.Public ignored -> {
            }
            case NodeOwner.Unowned ignored -> throw new NodeException("node.harvest.unowned");
            case NodeOwner.Business owner -> {
                Business business = deps.businesses().find(owner.businessId())
                        .orElseThrow(() -> new NodeException("node.harvest.unowned"));
                if (!business.isMember(character)) {
                    throw new NodeException("node.harvest.not-member", Map.of("business", business.displayName()));
                }
            }
        }
    }

    /**
     * Gathers once: checks, pays, rolls and fires the harvest event.
     *
     * @param tierNumber the tier the player picked
     * @return what was paid and rolled, for the caller to hand over
     * @throws NodeException when the node, the tier, the character's access, level or money refuse it;
     *                       nothing is charged then
     */
    public Harvest harvest(Node node, CharacterRef character, int tierNumber) {
        RandomTable table = table(node);
        RandomTier tier = table.tier(tierNumber).filter(found -> found.tier() <= node.tier())
                .orElseThrow(() -> new NodeException("node.harvest.no-tier", Map.of("tier", String.valueOf(tierNumber))));
        requireAccess(node, character);
        String level = requiredLevel(tier);
        if (!deps.levels().reaches(character, table.skill(), level)) {
            throw new NodeException("node.harvest.low-level", Map.of("level", level, "skill", table.skill()));
        }
        HarvestCost cost = cost(node, tier);
        pay(character, cost);
        List<ItemAmount> items = roller.roll(tier.output());
        Harvest harvest = new Harvest(node, character, tier.tier(), cost, items);
        deps.events().harvested(harvest);
        return harvest;
    }

    /**
     * Checks every part of the cost first, then takes it. Downtime can't be given back, so it is
     * taken last; tokens and money are refunded if a later part fails.
     */
    private void pay(CharacterRef character, HarvestCost cost) {
        UUID player = character.player();
        long balance = deps.tokens().balance(character);
        if (balance < cost.harvestTokens()) {
            throw new NodeException("node.harvest.no-tokens", Map.of(
                    "cost", String.valueOf(cost.harvestTokens()), "balance", String.valueOf(balance)));
        }
        if (deps.wallet().balance(player).compareTo(cost.money()) < 0) {
            throw new NodeException("node.harvest.no-money", Map.of("cost", deps.wallet().format(cost.money())));
        }
        if (cost.downtime() > 0 && deps.downtime().available(player) < cost.downtime()) {
            throw new NodeException("node.harvest.no-downtime", Map.of("cost", String.valueOf(cost.downtime())));
        }
        if (cost.harvestTokens() > 0) {
            deps.tokens().remove(character, cost.harvestTokens());
        }
        if (cost.money().signum() > 0 && !deps.wallet().withdraw(player, cost.money())) {
            refundTokens(character, cost);
            throw new NodeException("node.harvest.no-money", Map.of("cost", deps.wallet().format(cost.money())));
        }
        if (cost.downtime() > 0 && !deps.downtime().trySpend(player, cost.downtime())) {
            refundTokens(character, cost);
            if (cost.money().signum() > 0) {
                deps.wallet().deposit(player, cost.money());
            }
            throw new NodeException("node.harvest.no-downtime", Map.of("cost", String.valueOf(cost.downtime())));
        }
    }

    private void refundTokens(CharacterRef character, HarvestCost cost) {
        if (cost.harvestTokens() > 0) {
            deps.tokens().add(character, cost.harvestTokens());
        }
    }
}
