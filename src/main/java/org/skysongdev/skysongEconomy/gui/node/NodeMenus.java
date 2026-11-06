package org.skysongdev.skysongEconomy.gui.node;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.character.SkillLevels;
import org.skysongdev.skysongEconomy.gui.MenuTheme;
import org.skysongdev.skysongEconomy.items.ItemHandOver;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeKind;
import org.skysongdev.skysongEconomy.nodes.NodeMenuIcons;
import org.skysongdev.skysongEconomy.nodes.NodeService;
import org.skysongdev.skysongEconomy.nodes.NodeSettingsParser;
import org.skysongdev.skysongEconomy.nodes.harvest.Harvest;
import org.skysongdev.skysongEconomy.nodes.harvest.RandomHarvester;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * Opens node menus and runs harvests from them. Right-clicking a node only opens its menu;
 * nothing is given until the player clicks a tier.
 */
public final class NodeMenus {
    /**
     * What the menus read and use.
     *
     * @param nodes      the placed nodes
     * @param harvester  gathers at random nodes
     * @param businesses the owners of business nodes, for the owner line
     * @param characters which character a player is playing
     * @param tokens     Harvest Token balances, shown in the menu
     * @param skills     skill names
     * @param levels     skill level names and characters' levels
     * @param wallet     how money is written
     * @param handOver   gives the harvested items
     */
    public record Dependencies(NodeService nodes, RandomHarvester harvester, BusinessService businesses,
                               CharacterDirectory characters, GatheringTokens tokens, SkillCatalog skills,
                               SkillLevels levels, Wallet wallet, ItemHandOver handOver) {
        public Dependencies {
            Objects.requireNonNull(nodes, "nodes");
            Objects.requireNonNull(harvester, "harvester");
            Objects.requireNonNull(businesses, "businesses");
            Objects.requireNonNull(characters, "characters");
            Objects.requireNonNull(tokens, "tokens");
            Objects.requireNonNull(skills, "skills");
            Objects.requireNonNull(levels, "levels");
            Objects.requireNonNull(wallet, "wallet");
            Objects.requireNonNull(handOver, "handOver");
        }
    }

    /** The menu items, resolved from {@code nodes/settings.yml}. */
    record Icons(Material info, Material tier, Material lockedTier) {
    }

    private final Dependencies deps;
    private final MenuTheme theme;
    private final Messenger messenger;
    private final Icons icons;
    private final Logger logger;

    public NodeMenus(Dependencies deps, MenuTheme theme, Messenger messenger, NodeMenuIcons icons, Logger logger) {
        this.deps = Objects.requireNonNull(deps, "deps");
        this.theme = theme;
        this.messenger = messenger;
        this.logger = logger;
        this.icons = new Icons(MenuTheme.material(icons.info(), NodeSettingsParser.FILE, logger),
                MenuTheme.material(icons.tier(), NodeSettingsParser.FILE, logger),
                MenuTheme.material(icons.lockedTier(), NodeSettingsParser.FILE, logger));
    }

    /** Opens the node's menu, or tells the player why they can't use the node. */
    public void open(Player player, Node node) {
        if (node.kind() == NodeKind.FARM) {
            messenger.send(player, "node.harvest.farm-not-ready");
            return;
        }
        Optional<CharacterRef> character = deps.characters().active(player.getUniqueId());
        if (character.isEmpty()) {
            messenger.send(player, "command.you-have-no-character");
            return;
        }
        try {
            deps.harvester().table(node);
            deps.harvester().requireAccess(node, character.get());
        } catch (RefusedException refusal) {
            messenger.send(player, refusal);
            return;
        }
        new NodeGatherMenu(this, node.id(), character.get()).open(player);
    }

    /**
     * Gathers once at the tier the player clicked, gives the items and lists them in chat.
     * Everything is checked again, since the menu may have stayed open while things changed.
     */
    void harvest(Player player, String nodeId, int tier) {
        Optional<Node> node = deps.nodes().find(nodeId);
        Optional<CharacterRef> character = deps.characters().active(player.getUniqueId());
        if (node.isEmpty() || character.isEmpty()) {
            player.closeInventory();
            messenger.send(player, node.isEmpty() ? "node.harvest.gone" : "command.you-have-no-character");
            return;
        }
        Harvest harvest;
        try {
            harvest = deps.harvester().harvest(node.get(), character.get(), tier);
        } catch (RefusedException refusal) {
            messenger.send(player, refusal);
            return;
        }
        give(player, harvest);
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
}
