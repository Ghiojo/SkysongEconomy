package org.skysongdev.skysongEconomy.commands.ledger;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.PriceExpression;

import java.math.BigDecimal;
import java.util.Map;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se ledger list: every item's base value, alphabetically. */
final class ListLedger implements SubCommand {
    private final LedgerSupport support;

    ListLedger(LedgerSupport support) {
        this.support = support;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String usage() {
        return "";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Map<String, PriceExpression> all = support.prices.all();
        support.messenger.send(sender, "ledger.list-header", unparsed("count", String.valueOf(all.size())));
        all.forEach((id, expression) -> {
            try {
                BigDecimal value = support.prices.value(id).orElseThrow();
                support.messenger.send(sender, "ledger.list-entry", unparsed("material", id), support.value(value),
                        unparsed("expression", expression.text()));
            } catch (PriceException broken) {
                support.messenger.send(sender, "ledger.list-entry-broken", unparsed("material", id),
                        unparsed("expression", expression.text()));
            }
        });
    }
}
