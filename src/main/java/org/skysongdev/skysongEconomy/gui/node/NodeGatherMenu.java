package org.skysongdev.skysongEconomy.gui.node;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.character.CharacterRef;
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
 * The menu of a random node: the node's details on top, one button per tier it offers, and the
 * booster slot. Opening it gives nothing; each click on a tier is one instant harvest, and the
 * menu stays open so the player can click again. Tiers the player can't use show why on a
 * greyed-out icon.
 */
final class NodeGatherMenu extends HarvestMenu {
    NodeGatherMenu(NodeMenus menus, String nodeId, CharacterRef character) {
        super(menus, nodeId, character);
    }

    @Override
    protected void drawNode(Node node) {
        set(INFO, MenuButton.display(info(node)));
        RandomHarvester harvester = menus.deps().harvester();
        RandomTable table;
        try {
            table = harvester.table(node);
        } catch (RefusedException unavailable) {
            return;
        }
        for (RandomTier tier : harvester.offered(node)) {
            set(FIRST_TIER + tier.tier() - 1, tierButton(node, table, tier));
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
        return tierButton(tier.tier(), cost, harvester.requiredLevel(tier), table.skill(), tier.output());
    }

    private Component line(String key, TagResolver... placeholders) {
        return menus.text().line(key, placeholders);
    }
}
