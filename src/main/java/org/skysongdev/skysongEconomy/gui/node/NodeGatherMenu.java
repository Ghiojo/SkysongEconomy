package org.skysongdev.skysongEconomy.gui.node;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.gui.Menu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;
import org.skysongdev.skysongEconomy.nodes.harvest.RandomHarvester;
import org.skysongdev.skysongEconomy.nodes.random.RandomTable;
import org.skysongdev.skysongEconomy.nodes.random.RandomTier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * The menu of a random node: the node's details on top, and one button per tier it offers.
 * Opening it gives nothing; each click on a tier is one instant harvest, and the menu stays open
 * so the player can click again. Tiers the player can't use show why on a greyed-out icon.
 */
final class NodeGatherMenu extends Menu {
    private static final int SIZE = 27;
    private static final int INFO = 4;
    /** Tier N goes in slot FIRST_TIER + N - 1, so tiers 1 to 5 fill the middle of the second row. */
    private static final int FIRST_TIER = 11;
    private static final int CLOSE = 22;

    private final NodeMenus menus;
    private final String nodeId;
    private final CharacterRef character;

    NodeGatherMenu(NodeMenus menus, String nodeId, CharacterRef character) {
        super(SIZE, menus.messenger().render("node.gather.title",
                parsed("node", menus.deps().nodes().find(nodeId).map(menus.deps().nodes()::name).orElse(nodeId))));
        this.menus = menus;
        this.nodeId = nodeId;
        this.character = character;
    }

    @Override
    protected void draw() {
        MenuButton border = menus.theme().border();
        for (int slot = 0; slot < SIZE; slot++) {
            set(slot, border);
        }
        set(CLOSE, menus.theme().close());
        Optional<Node> node = menus.deps().nodes().find(nodeId);
        if (node.isEmpty()) {
            return;
        }
        set(INFO, MenuButton.display(info(node.get())));
        RandomHarvester harvester = menus.deps().harvester();
        RandomTable table;
        try {
            table = harvester.table(node.get());
        } catch (RefusedException unavailable) {
            return;
        }
        for (RandomTier tier : harvester.offered(node.get())) {
            set(FIRST_TIER + tier.tier() - 1, tierButton(node.get(), table, tier));
        }
    }

    private ItemStack info(Node node) {
        List<Component> lore = new ArrayList<>();
        lore.add(line("node.gather.info.owner", component("owner", menus.text().owner(node))));
        lore.add(line("node.gather.info.tokens",
                unparsed("tokens", String.valueOf(menus.deps().tokens().balance(character)))));
        lore.add(line("node.gather.info.hint"));
        return menus.theme().item(menus.icons().info(), menus.messenger().render("node.gather.info.name",
                parsed("node", menus.deps().nodes().name(node)), unparsed("tier", String.valueOf(node.tier()))), lore);
    }

    private MenuButton tierButton(Node node, RandomTable table, RandomTier tier) {
        RandomHarvester harvester = menus.deps().harvester();
        Optional<HarvestCost> cost;
        try {
            cost = Optional.of(harvester.cost(node, tier));
        } catch (RefusedException businessOnly) {
            cost = Optional.empty();
        }
        return menus.text().tierButton(character, tier.tier(), cost, harvester.requiredLevel(tier), table.skill(),
                tier.output(), click -> {
                    menus.harvest(click.player(), nodeId, tier.tier());
                    // The token balance and the player's level may have changed.
                    redraw();
                });
    }

    private Component line(String key, TagResolver... placeholders) {
        return menus.text().line(key, placeholders);
    }
}
