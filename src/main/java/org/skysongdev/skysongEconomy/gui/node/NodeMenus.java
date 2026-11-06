package org.skysongdev.skysongEconomy.gui.node;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.character.SkillLevels;
import org.skysongdev.skysongEconomy.gui.MenuTheme;
import org.skysongdev.skysongEconomy.items.ItemHandOver;
import org.skysongdev.skysongEconomy.items.ItemStackBytes;
import org.skysongdev.skysongEconomy.items.PrimerItems;
import org.skysongdev.skysongEconomy.items.StackTaking;
import org.skysongdev.skysongEconomy.items.StoredItems;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeKind;
import org.skysongdev.skysongEconomy.nodes.NodeMenuIcons;
import org.skysongdev.skysongEconomy.nodes.NodeService;
import org.skysongdev.skysongEconomy.nodes.NodeSettingsParser;
import org.skysongdev.skysongEconomy.nodes.farm.FarmHarvester;
import org.skysongdev.skysongEconomy.nodes.farm.FarmPlots;
import org.skysongdev.skysongEconomy.nodes.farm.FarmResource;
import org.skysongdev.skysongEconomy.nodes.harvest.Harvest;
import org.skysongdev.skysongEconomy.nodes.harvest.RandomHarvester;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * Opens node menus and runs what is clicked in them: harvests at random and farm nodes, and
 * planting and taking out primers at farm nodes. Right-clicking a node only opens its menu;
 * nothing is given until the player clicks a tier.
 */
public final class NodeMenus {
    /**
     * What the menus read and use.
     *
     * @param nodes      the placed nodes
     * @param harvester  gathers at random nodes
     * @param farms      plants, takes out and harvests at farm nodes
     * @param businesses the owners of business nodes, for the owner line
     * @param characters which character a player is playing
     * @param tokens     Harvest Token balances, shown in the menu
     * @param skills     skill names
     * @param levels     skill level names and characters' levels
     * @param wallet     how money is written
     * @param handOver   gives the harvested items
     */
    public record Dependencies(NodeService nodes, RandomHarvester harvester, Farms farms, BusinessService businesses,
                               CharacterDirectory characters, GatheringTokens tokens, SkillCatalog skills,
                               SkillLevels levels, Wallet wallet, ItemHandOver handOver) {
        public Dependencies {
            Objects.requireNonNull(nodes, "nodes");
            Objects.requireNonNull(harvester, "harvester");
            Objects.requireNonNull(farms, "farms");
            Objects.requireNonNull(businesses, "businesses");
            Objects.requireNonNull(characters, "characters");
            Objects.requireNonNull(tokens, "tokens");
            Objects.requireNonNull(skills, "skills");
            Objects.requireNonNull(levels, "levels");
            Objects.requireNonNull(wallet, "wallet");
            Objects.requireNonNull(handOver, "handOver");
        }
    }

    /**
     * What farm node menus use.
     *
     * @param plots     what is planted where, planting and taking out
     * @param harvester harvests farm nodes
     * @param primers   reads which items are primers
     */
    public record Farms(FarmPlots plots, FarmHarvester harvester, PrimerItems primers) {
        public Farms {
            Objects.requireNonNull(plots, "plots");
            Objects.requireNonNull(harvester, "harvester");
            Objects.requireNonNull(primers, "primers");
        }
    }

    /** The menu items, resolved from {@code nodes/settings.yml}. */
    record Icons(Material info, Material tier, Material lockedTier, Material emptyPlot, Material takeOut) {
    }

    private final Dependencies deps;
    private final MenuTheme theme;
    private final Messenger messenger;
    private final Icons icons;
    private final Logger logger;
    private final NodeMenuText text;

    public NodeMenus(Dependencies deps, MenuTheme theme, Messenger messenger, NodeMenuIcons icons, Logger logger) {
        this.deps = Objects.requireNonNull(deps, "deps");
        this.theme = theme;
        this.messenger = messenger;
        this.logger = logger;
        this.icons = new Icons(MenuTheme.material(icons.info(), NodeSettingsParser.FILE, logger),
                MenuTheme.material(icons.tier(), NodeSettingsParser.FILE, logger),
                MenuTheme.material(icons.lockedTier(), NodeSettingsParser.FILE, logger),
                MenuTheme.material(icons.emptyPlot(), NodeSettingsParser.FILE, logger),
                MenuTheme.material(icons.takeOut(), NodeSettingsParser.FILE, logger));
        this.text = new NodeMenuText(this);
    }

    /** Opens the node's menu, or tells the player why they can't use the node. */
    public void open(Player player, Node node) {
        Optional<CharacterRef> character = deps.characters().active(player.getUniqueId());
        if (character.isEmpty()) {
            messenger.send(player, "command.you-have-no-character");
            return;
        }
        try {
            if (node.kind() == NodeKind.FARM) {
                requireFarmUser(node, character.get());
                new FarmMenu(this, node.id(), character.get()).open(player);
            } else {
                deps.harvester().table(node);
                deps.harvester().requireAccess(node, character.get());
                new NodeGatherMenu(this, node.id(), character.get()).open(player);
            }
        } catch (RefusedException refusal) {
            messenger.send(player, refusal);
        }
    }

    /** A farm menu is for the business's members, and for whoever may tend its farms. */
    private void requireFarmUser(Node node, CharacterRef character) {
        Farms farms = deps.farms();
        farms.plots().type(node);
        if (!farms.plots().canTend(node, character)) {
            farms.harvester().requireAccess(node, character);
        }
    }

    /**
     * Gathers once at the tier the player clicked, gives the items and lists them in chat.
     * Everything is checked again, since the menu may have stayed open while things changed.
     */
    void harvest(Player player, String nodeId, int tier) {
        act(player, nodeId, (node, character) -> give(player, node.kind() == NodeKind.FARM
                ? deps.farms().harvester().harvest(node, character, tier)
                : deps.harvester().harvest(node, character, tier)));
    }

    /**
     * Plants the stack the player clicked in their inventory, taking as many similar stacks as
     * the resource's primer needs. Nothing leaves the inventory unless planting succeeds.
     */
    void plant(Player player, String nodeId, int inventorySlot) {
        ItemStack clicked = player.getInventory().getItem(inventorySlot);
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        act(player, nodeId, (node, character) -> {
            Farms farms = deps.farms();
            FarmResource resource = farms.plots().resourceFor(node, farms.primers().primerTag(clicked),
                    farms.primers().materialId(clicked));
            StackTaking taking = StackTaking.plan(player.getInventory(), inventorySlot, resource.primer().amount());
            if (taking.isEmpty()) {
                messenger.send(player, "node.farm.not-enough", unparsed("amount", String.valueOf(resource.primer().amount())),
                        parsed("resource", resource.name()));
                return;
            }
            farms.plots().plant(node, character, resource, ItemStackBytes.store(taking.stacks()));
            taking.takeFrom(player.getInventory());
            messenger.send(player, "node.farm.planted", parsed("resource", resource.name()));
        });
    }

    /** Takes the primer out, charging the removal downtime, and gives the exact items back. */
    void takeOut(Player player, String nodeId) {
        act(player, nodeId, (node, character) -> {
            StoredItems primer = deps.farms().plots().takeOut(node, character);
            boolean dropped = deps.handOver().giveStacks(player, ItemStackBytes.restore(primer));
            messenger.send(player, dropped ? "node.farm.taken-out-dropped" : "node.farm.taken-out");
        });
    }

    /** Runs a menu action on the node and the player's character, telling them why when it's refused. */
    private void act(Player player, String nodeId, BiConsumer<Node, CharacterRef> action) {
        Optional<Node> node = deps.nodes().find(nodeId);
        Optional<CharacterRef> character = deps.characters().active(player.getUniqueId());
        if (node.isEmpty() || character.isEmpty()) {
            player.closeInventory();
            messenger.send(player, node.isEmpty() ? "node.harvest.gone" : "command.you-have-no-character");
            return;
        }
        try {
            action.accept(node.get(), character.get());
        } catch (RefusedException refusal) {
            messenger.send(player, refusal);
        }
    }

    private void give(Player player, Harvest harvest) {
        if (harvest.items().isEmpty()) {
            messenger.send(player, "node.harvest.got-nothing");
            return;
        }
        boolean dropped;
        try {
            dropped = deps.handOver().give(player, harvest.items());
        } catch (IllegalArgumentException missingItem) {
            // Items are checked when the files load, so this only happens if an item file was removed since.
            logger.log(Level.WARNING, "A harvest at node " + harvest.node().id() + " named an item without a file", missingItem);
            messenger.send(player, "node.harvest.item-missing");
            return;
        }
        messenger.send(player, "node.harvest.got", component("items", itemList(harvest.items())));
        if (dropped) {
            messenger.send(player, "node.harvest.dropped");
        }
    }

    private Component itemList(List<ItemAmount> items) {
        return Component.join(JoinConfiguration.separator(messenger.render("node.harvest.item-separator")),
                items.stream().map(item -> messenger.render("node.harvest.item",
                        unparsed("amount", String.valueOf(item.amount())),
                        component("item", deps.handOver().name(item.materialId())))).toList());
    }

    Dependencies deps() {
        return deps;
    }

    MenuTheme theme() {
        return theme;
    }

    Messenger messenger() {
        return messenger;
    }

    Icons icons() {
        return icons;
    }

    NodeMenuText text() {
        return text;
    }
}
