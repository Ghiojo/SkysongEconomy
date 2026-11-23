package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.calendar.CalendarMoment;
import org.skysongdev.skysongEconomy.calendar.CalendarNames;
import org.skysongdev.skysongEconomy.gui.node.HarvestCostText;
import org.skysongdev.skysongEconomy.loot.ItemOdds;
import org.skysongdev.skysongEconomy.loot.LootOdds;
import org.skysongdev.skysongEconomy.messaging.NumberText;
import org.skysongdev.skysongEconomy.nodes.NodeException;
import org.skysongdev.skysongEconomy.nodes.modifiers.ActiveModifiers;
import org.skysongdev.skysongEconomy.nodes.modifiers.CalendarModifier;
import org.skysongdev.skysongEconomy.nodes.modifiers.NodeFacts;
import org.skysongdev.skysongEconomy.nodes.random.RandomTable;
import org.skysongdev.skysongEconomy.nodes.random.RandomTier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se node odds &lt;table&gt; &lt;tier&gt; [when]: for balancing, shows what a tier costs and needs,
 * each item's chance per harvest and average amount, and what one harvest is worth on average by
 * the price database. Boosters aren't counted.
 * <p>
 * With a time of year ({@code today}, a season, a day like {@code Rana 10}, or {@code festival}),
 * the calendar modifiers that hit the table then are counted too. For a whole season, only the
 * modifiers that list that season count, and modifiers that name single nodes never do.
 */
final class NodeOdds extends NodeSubCommand {
    private final OddsSources sources;
    private final ModifierSources modifiers;

    NodeOdds(NodeCommandSupport support, OddsSources sources, ModifierSources modifiers) {
        super(support, "odds", "<table> <tier> [today|season|day|festival]", 2);
        this.sources = sources;
        this.modifiers = modifiers;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String tableId = args[0].toLowerCase(Locale.ROOT);
        RandomTable table = sources.tables().random().find(tableId)
                .orElseThrow(() -> new NodeException("node.unknown-source.random", Map.of("source", args[0])));
        RandomTier tier = parseTier(args[1]).flatMap(table::tier).orElseThrow(() -> new NodeException("node.odds.no-tier",
                Map.of("tier", args[1], "tiers", table.tiers().keySet().stream().map(String::valueOf)
                        .collect(Collectors.joining(", ")))));

        String level = sources.settings().levelFor(tier.tier());
        support.messenger.send(sender, "node.odds.header",
                parsed("table", table.name()),
                unparsed("tier", String.valueOf(tier.tier())),
                parsed("level", sources.levels().levelName(level)),
                parsed("skill", sources.skills().displayName(table.skill())),
                unparsed("cost", String.valueOf(tier.cost())));
        tier.publicCost().ifPresentOrElse(
                cost -> support.messenger.send(sender, "node.odds.public-cost", component("public_cost", HarvestCostText.describe(support.messenger, sources.wallet(), cost))),
                () -> support.messenger.send(sender, "node.odds.business-only"));

        ActiveModifiers active = args.length > 2
                ? modifiersAt(table, tier, String.join(" ", Arrays.copyOfRange(args, 2, args.length)))
                : ActiveModifiers.NONE;
        if (active.dormant().isPresent()) {
            support.messenger.send(sender, "node.odds.dormant", parsed("modifier", active.dormant().get().name()));
            return;
        }
        for (CalendarModifier modifier : active.applied()) {
            support.messenger.send(sender, "node.odds.modifier", parsed("modifier", modifier.name()),
                    component("effect", ListNodeModifiers.effect(support.messenger, modifier)));
        }

        List<ItemOdds> odds = LootOdds.of(tier.output(), active.effect());
        for (ItemOdds item : odds) {
            support.messenger.send(sender, "node.odds.item",
                    unparsed("item", item.materialId()),
                    unparsed("chance", NumberText.percent(item.chance())),
                    unparsed("amount", NumberText.decimal(item.averageAmount())));
        }
        LootOdds.AverageValue value = LootOdds.averageValue(odds, sources.prices()::value);
        support.messenger.send(sender, "node.odds.value",
                unparsed("value", sources.priceSettings().truncate(value.value()).toPlainString()));
        if (!value.unpriced().isEmpty()) {
            support.messenger.send(sender, "node.odds.unpriced", unparsed("items", String.join(", ", value.unpriced())));
        }
    }

    /**
     * @return the modifiers hitting the table's tier at the time staff typed
     * @throws NodeException {@code node.modifiers.no-calendar} without SkysongCalendar, or
     *                       {@code node.odds.bad-when} when the time isn't understood
     */
    private ActiveModifiers modifiersAt(RandomTable table, RandomTier tier, String when) {
        CalendarNames names = modifiers.calendar().names()
                .orElseThrow(() -> new NodeException("node.modifiers.no-calendar"));
        CalendarMoment moment;
        try {
            moment = CalendarMoment.parse(when, names, modifiers.calendar().today());
        } catch (IllegalArgumentException e) {
            throw new NodeException("node.odds.bad-when", Map.of("problem", e.getMessage()));
        }
        // No node: a table on its own, so modifiers naming single nodes don't count.
        NodeFacts facts = NodeFacts.random("", table.id(), table.type());
        return modifiers.modifiers().activeFor(facts, tier.tier(), moment);
    }

    private static Optional<Integer> parseTier(String text) {
        try {
            return Optional.of(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** @return today, festival, then every season and month id */
    private List<String> whenChoices() {
        List<String> choices = new ArrayList<>(List.of(CalendarMoment.TODAY, CalendarMoment.FESTIVAL));
        modifiers.calendar().names().ifPresent(names -> {
            names.seasons().forEach(season -> choices.add(season.id()));
            names.months().forEach(month -> choices.add(month.id()));
        });
        return choices;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> sources.tables().random().all().stream().map(RandomTable::id)
                    .filter(id -> id.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
            case 2 -> sources.tables().random().find(args[0].toLowerCase(Locale.ROOT))
                    .map(table -> table.tiers().keySet().stream().map(String::valueOf).toList())
                    .orElse(List.of());
            case 3 -> whenChoices().stream().filter(choice -> choice.startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
            default -> List.of();
        };
    }
}
