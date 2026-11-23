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
import org.skysongdev.skysongEconomy.loot.YieldAdjuster;
import org.skysongdev.skysongEconomy.messaging.NumberText;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeOwner;
import org.skysongdev.skysongEconomy.nodes.modifiers.ActiveModifiers;
import org.skysongdev.skysongEconomy.nodes.modifiers.CalendarModifier;
import org.skysongdev.skysongEconomy.trade.IntRange;

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
     * @return a tier's button: its cost, the level it needs, the calendar modifiers on today and
     *         what it gives with them, greyed out with the reason when the player can't use it
     * @param onClick one harvest at the tier
     */
    MenuButton tierButton(CharacterRef character, TierChoice choice, Consumer<MenuClick> onClick) {
        TagResolver tierNumber = unparsed("tier", String.valueOf(choice.tier()));
        List<Component> lore = new ArrayList<>();
        Optional<Component> locked = Optional.empty();
        if (choice.cost().isPresent()) {
            lore.add(line("node.gather.tier.cost",
                    component("cost", HarvestCostText.describe(menus.messenger(), menus.deps().wallet(), choice.cost().get()))));
        } else {
            locked = Optional.of(line("node.gather.tier.business-only"));
        }
        lore.add(line("node.gather.tier.needs", parsed("level", menus.deps().levels().levelName(choice.level())),
                parsed("skill", menus.deps().skills().displayName(choice.skill()))));
        if (locked.isEmpty() && !menus.deps().levels().reaches(character, choice.skill(), choice.level())) {
            locked = Optional.of(line("node.gather.tier.too-low"));
        }
        ActiveModifiers modifiers = choice.modifiers();
        Optional<CalendarModifier> dormant = modifiers.dormant();
        if (dormant.isPresent()) {
            locked = Optional.of(line("node.gather.tier.dormant", parsed("modifier", dormant.get().name())));
        }
        lore.addAll(modifierLines(modifiers));
        lore.addAll(outputLines(choice.output(), modifiers.effect()));
        if (locked.isPresent()) {
            lore.add(locked.get());
            return MenuButton.display(menus.theme().item(menus.icons().lockedTier(),
                    line("node.gather.tier.name-locked", tierNumber), lore));
        }
        lore.add(line("node.gather.tier.click"));
        return MenuButton.of(menus.theme().item(menus.icons().tier(), line("node.gather.tier.name", tierNumber), lore),
                onClick);
    }

    /** @return one line per calendar modifier on: its name and what it changes */
    private List<Component> modifierLines(ActiveModifiers modifiers) {
        List<Component> lines = new ArrayList<>();
        for (CalendarModifier modifier : modifiers.applied()) {
            if (modifier.dormant()) {
                continue;
            }
            lines.add(line("node.gather.tier.modifier", parsed("modifier", modifier.name()),
                    unparsed("effect", modifier.effect().text())));
        }
        return lines;
    }

    /**
     * @param adjuster what changes the harvest, such as the calendar modifiers on today; the lines
     *                 show the changed amounts, chances and roll counts
     * @return what a tier gives: guaranteed items, chance items, then each roll's entries and their odds
     */
    List<Component> outputLines(LootOutput output, YieldAdjuster adjuster) {
        List<Component> lines = new ArrayList<>();
        for (LootEntry.Item item : output.guaranteed()) {
            lines.add(line("node.gather.tier.guaranteed", itemName(item), amount(item, adjuster)));
        }
        for (ChanceItem chance : output.chances()) {
            lines.add(line("node.gather.tier.chance", itemName(chance.item()), amount(chance.item(), adjuster),
                    unparsed("chance", NumberText.percent(adjuster.chance(chance.percent()) / 100))));
        }
        for (GroupRolls rolls : output.rolls()) {
            IntRange times = new IntRange(Math.max(0, adjuster.rolls(rolls.count().min())),
                    Math.max(0, adjuster.rolls(rolls.count().max())));
            lines.add(line("node.gather.tier.rolls", unparsed("times", times.toString())));
            for (WeightedEntry entry : rolls.group().entries()) {
                lines.add(line("node.gather.tier.roll-entry", component("entry", entryName(entry.entry(), adjuster)),
                        unparsed("chance", NumberText.percent(rolls.group().chance(entry)))));
            }
        }
        return lines;
    }

    private static TagResolver amount(LootEntry.Item item, YieldAdjuster adjuster) {
        return unparsed("amount", adjuster.amount(item.amount()).toString());
    }

    private Component entryName(LootEntry entry, YieldAdjuster adjuster) {
        return switch (entry) {
            case LootEntry.Item item -> line("node.gather.tier.entry-item", itemName(item), amount(item, adjuster));
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
