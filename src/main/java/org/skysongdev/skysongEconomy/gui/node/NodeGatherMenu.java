package org.skysongdev.skysongEconomy.gui.node;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.gui.Menu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.loot.ChanceItem;
import org.skysongdev.skysongEconomy.loot.GroupRolls;
import org.skysongdev.skysongEconomy.loot.LootEntry;
import org.skysongdev.skysongEconomy.loot.LootOutput;
import org.skysongdev.skysongEconomy.loot.WeightedEntry;
import org.skysongdev.skysongEconomy.messaging.NumberText;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeOwner;
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
        lore.add(line("node.gather.info.owner", component("owner", owner(node))));
        lore.add(line("node.gather.info.tokens",
                unparsed("tokens", String.valueOf(menus.deps().tokens().balance(character)))));
        lore.add(line("node.gather.info.hint"));
        return menus.theme().item(menus.icons().info(), menus.messenger().render("node.gather.info.name",
                parsed("node", menus.deps().nodes().name(node)), unparsed("tier", String.valueOf(node.tier()))), lore);
    }

    private Component owner(Node node) {
        return switch (node.owner()) {
            case NodeOwner.Public ignored -> menus.messenger().render("node.owner-public");
            case NodeOwner.Unowned ignored -> menus.messenger().render("node.owner-unowned");
            case NodeOwner.Business business -> Component.text(menus.deps().businesses().find(business.businessId())
                    .map(Business::displayName).orElse(business.businessId()));
        };
    }

    private MenuButton tierButton(Node node, RandomTable table, RandomTier tier) {
        RandomHarvester harvester = menus.deps().harvester();
        TagResolver tierNumber = unparsed("tier", String.valueOf(tier.tier()));
        String level = harvester.requiredLevel(tier);
        List<Component> lore = new ArrayList<>();
        Optional<String> locked = Optional.empty();
        try {
            HarvestCost cost = harvester.cost(node, tier);
            lore.add(line("node.gather.tier.cost",
                    component("cost", HarvestCostText.describe(menus.messenger(), menus.deps().wallet(), cost))));
        } catch (RefusedException businessOnly) {
            locked = Optional.of("node.gather.tier.business-only");
        }
        lore.add(line("node.gather.tier.needs", parsed("level", menus.deps().levels().levelName(level)),
                parsed("skill", menus.deps().skills().displayName(table.skill()))));
        if (locked.isEmpty() && !menus.deps().levels().reaches(character, table.skill(), level)) {
            locked = Optional.of("node.gather.tier.too-low");
        }
        lore.addAll(outputLines(tier.output()));
        if (locked.isPresent()) {
            lore.add(line(locked.get()));
            return MenuButton.display(menus.theme().item(menus.icons().lockedTier(),
                    menus.messenger().render("node.gather.tier.name-locked", tierNumber), lore));
        }
        lore.add(line("node.gather.tier.click"));
        Material icon = menus.icons().tier();
        return MenuButton.of(menus.theme().item(icon, menus.messenger().render("node.gather.tier.name", tierNumber), lore),
                click -> {
                    menus.harvest(click.player(), nodeId, tier.tier());
                    // The token balance and the player's level may have changed.
                    redraw();
                });
    }

    /** @return what the tier gives: guaranteed items, chance items, then each roll's entries and their odds */
    private List<Component> outputLines(LootOutput output) {
        List<Component> lines = new ArrayList<>();
        for (LootEntry.Item item : output.guaranteed()) {
            lines.add(line("node.gather.tier.guaranteed", itemName(item), unparsed("amount", item.amount().toString())));
        }
        for (ChanceItem chance : output.chances()) {
            lines.add(line("node.gather.tier.chance", itemName(chance.item()),
                    unparsed("amount", chance.item().amount().toString()),
                    unparsed("chance", NumberText.percent(chance.percent() / 100))));
        }
        for (GroupRolls rolls : output.rolls()) {
            lines.add(line("node.gather.tier.rolls", unparsed("times", rolls.count().toString())));
            for (WeightedEntry entry : rolls.group().entries()) {
                lines.add(line("node.gather.tier.roll-entry", component("entry", entryName(entry.entry())),
                        unparsed("chance", NumberText.percent(rolls.group().chance(entry)))));
            }
        }
        return lines;
    }

    private Component entryName(LootEntry entry) {
        return switch (entry) {
            case LootEntry.Item item -> menus.messenger().render("node.gather.tier.entry-item", itemName(item),
                    unparsed("amount", item.amount().toString()));
            case LootEntry.Group group -> group.group().isNamed()
                    ? menus.messenger().render("node.gather.tier.entry-group", unparsed("group", group.group().id()))
                    : menus.messenger().render("node.gather.tier.entry-mixed");
            case LootEntry.Nothing ignored -> menus.messenger().render("node.gather.tier.entry-nothing");
        };
    }

    private TagResolver itemName(LootEntry.Item item) {
        return component("item", menus.deps().handOver().name(item.materialId()));
    }

    private Component line(String key, TagResolver... placeholders) {
        return menus.messenger().render(key, placeholders);
    }
}
