package org.skysongdev.skysongEconomy.commands.node;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.loot.ItemOdds;
import org.skysongdev.skysongEconomy.loot.LootOdds;
import org.skysongdev.skysongEconomy.nodes.NodeException;
import org.skysongdev.skysongEconomy.nodes.random.PublicCost;
import org.skysongdev.skysongEconomy.nodes.random.RandomTable;
import org.skysongdev.skysongEconomy.nodes.random.RandomTier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
    private static final int DECIMALS = 2;
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
                cost -> support.messenger.send(sender, "node.odds.public-cost", component("public_cost", describe(cost))),
                () -> support.messenger.send(sender, "node.odds.business-only"));

        List<ItemOdds> odds = LootOdds.of(tier.output());
        for (ItemOdds item : odds) {
            support.messenger.send(sender, "node.odds.item",
                    unparsed("item", item.materialId()),
                    unparsed("chance", percent(item.chance())),
                    unparsed("amount", decimal(item.averageAmount())));
        }
        LootOdds.AverageValue value = LootOdds.averageValue(odds, sources.prices()::value);
        support.messenger.send(sender, "node.odds.value",
                unparsed("value", sources.priceSettings().truncate(value.value()).toPlainString()));
        if (!value.unpriced().isEmpty()) {
            support.messenger.send(sender, "node.odds.unpriced", unparsed("items", String.join(", ", value.unpriced())));
        }
    }

    /** @return every part of the cost, such as "1 downtime, 5 coins" */
    private Component describe(PublicCost cost) {
        if (cost.isFree()) {
            return support.messenger.render("node.cost.free");
        }
        List<Component> parts = new ArrayList<>();
        if (cost.downtime() > 0) {
            parts.add(support.messenger.render("node.cost.downtime", unparsed("amount", String.valueOf(cost.downtime()))));
        }
        if (cost.harvestTokens() > 0) {
            parts.add(support.messenger.render("node.cost.harvest-tokens",
                    unparsed("amount", String.valueOf(cost.harvestTokens()))));
        }
        if (cost.money().signum() > 0) {
            parts.add(support.messenger.render("node.cost.money", unparsed("amount", sources.wallet().format(cost.money()))));
        }
        return Component.join(JoinConfiguration.separator(support.messenger.render("node.cost.separator")), parts);
    }

    private static Optional<Integer> parseTier(String text) {
        try {
            return Optional.of(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** @return the chance as a percent, such as "12.5"; "<0.01" when it rounds to nothing but isn't 0 */
    private static String percent(double chance) {
        String text = decimal(chance * 100);
        return chance > 0 && text.equals("0") ? "<0.01" : text;
    }

    private static String decimal(double value) {
        return BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
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
