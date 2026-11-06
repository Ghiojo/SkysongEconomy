package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.gui.node.HarvestCostText;
import org.skysongdev.skysongEconomy.loot.ItemOdds;
import org.skysongdev.skysongEconomy.loot.LootOdds;
import org.skysongdev.skysongEconomy.messaging.NumberText;
import org.skysongdev.skysongEconomy.nodes.NodeException;
import org.skysongdev.skysongEconomy.nodes.random.RandomTable;
import org.skysongdev.skysongEconomy.nodes.random.RandomTier;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se node odds &lt;table&gt; &lt;tier&gt;: for balancing, shows what a tier costs and needs, each
 * item's chance per harvest and average amount, and what one harvest is worth on average by the
 * price database. Boosters aren't counted.
 */
final class NodeOdds extends NodeSubCommand {
    private final OddsSources sources;

    NodeOdds(NodeCommandSupport support, OddsSources sources) {
        super(support, "odds", "<table> <tier>", 2);
        this.sources = sources;
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

        List<ItemOdds> odds = LootOdds.of(tier.output());
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

    private static Optional<Integer> parseTier(String text) {
        try {
            return Optional.of(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> sources.tables().random().all().stream().map(RandomTable::id)
                    .filter(id -> id.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
            case 2 -> sources.tables().random().find(args[0].toLowerCase(Locale.ROOT))
                    .map(table -> table.tiers().keySet().stream().map(String::valueOf).toList())
                    .orElse(List.of());
            default -> List.of();
        };
    }
}
