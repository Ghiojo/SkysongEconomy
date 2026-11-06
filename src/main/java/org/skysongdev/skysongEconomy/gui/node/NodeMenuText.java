package org.skysongdev.skysongEconomy.gui.node;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.MenuClick;
import org.skysongdev.skysongEconomy.loot.ChanceItem;
import org.skysongdev.skysongEconomy.loot.GroupRolls;
import org.skysongdev.skysongEconomy.loot.LootEntry;
import org.skysongdev.skysongEconomy.loot.LootOutput;
import org.skysongdev.skysongEconomy.loot.WeightedEntry;
import org.skysongdev.skysongEconomy.messaging.NumberText;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeOwner;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** The parts random and farm node menus share: the owner line and the tier buttons. */
final class NodeMenuText {
    private final NodeMenus menus;

    NodeMenuText(NodeMenus menus) {
        this.menus = menus;
    }

    /** @return a line from {@code messages.yml} */
    Component line(String key, TagResolver... placeholders) {
        return menus.messenger().render(key, placeholders);
    }

    /** @return who owns the node: a business's name, public or unowned */
    Component owner(Node node) {
        return switch (node.owner()) {
            case NodeOwner.Public ignored -> line("node.owner-public");
            case NodeOwner.Unowned ignored -> line("node.owner-unowned");
            case NodeOwner.Business business -> Component.text(menus.deps().businesses().find(business.businessId())
                    .map(Business::displayName).orElse(business.businessId()));
        };
    }

    /**
     * @return a tier's button: its cost, the level it needs and what it gives, greyed out with the
     *         reason when the player can't use it
     * @param cost    what one harvest costs; empty when the tier isn't offered on this node, such as
     *                a business-only tier on a public node
     * @param onClick one harvest at the tier
     */
    MenuButton tierButton(CharacterRef character, int tier, Optional<HarvestCost> cost, String level, String skill,
                          LootOutput output, Consumer<MenuClick> onClick) {
        TagResolver tierNumber = unparsed("tier", String.valueOf(tier));
        List<Component> lore = new ArrayList<>();
        Optional<String> locked = Optional.empty();
        if (cost.isPresent()) {
            lore.add(line("node.gather.tier.cost",
                    component("cost", HarvestCostText.describe(menus.messenger(), menus.deps().wallet(), cost.get()))));
        } else {
            locked = Optional.of("node.gather.tier.business-only");
        }
        lore.add(line("node.gather.tier.needs", parsed("level", menus.deps().levels().levelName(level)),
                parsed("skill", menus.deps().skills().displayName(skill))));
        if (locked.isEmpty() && !menus.deps().levels().reaches(character, skill, level)) {
            locked = Optional.of("node.gather.tier.too-low");
        }
        lore.addAll(outputLines(output));
        if (locked.isPresent()) {
            lore.add(line(locked.get()));
            return MenuButton.display(menus.theme().item(menus.icons().lockedTier(),
                    line("node.gather.tier.name-locked", tierNumber), lore));
        }
        lore.add(line("node.gather.tier.click"));
        return MenuButton.of(menus.theme().item(menus.icons().tier(), line("node.gather.tier.name", tierNumber), lore),
                onClick);
    }

    /** @return what a tier gives: guaranteed items, chance items, then each roll's entries and their odds */
    List<Component> outputLines(LootOutput output) {
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
            case LootEntry.Item item -> line("node.gather.tier.entry-item", itemName(item),
                    unparsed("amount", item.amount().toString()));
            case LootEntry.Group group -> group.group().isNamed()
                    ? line("node.gather.tier.entry-group", unparsed("group", group.group().id()))
                    : line("node.gather.tier.entry-mixed");
            case LootEntry.Nothing ignored -> line("node.gather.tier.entry-nothing");
        };
    }

    private TagResolver itemName(LootEntry.Item item) {
        return component("item", menus.deps().handOver().name(item.materialId()));
    }
}
