package org.skysongdev.skysongEconomy.commands.ledger;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.prices.PriceExpression;

import java.util.Optional;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se ledger view [materialId]: shows an item's base value and how it is worked out. */
final class ViewLedger implements SubCommand {
    private final LedgerSupport support;

    ViewLedger(LedgerSupport support) {
        this.support = support;
    }

    @Override
    public String name() {
        return "view";
    }

    @Override
    public String usage() {
        return "[materialId]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        support.materialId(sender, args, 0).ifPresent(id -> support.run(sender, "show the base value of " + id, () -> {
            Optional<PriceExpression> expression = support.prices.expression(id);
            if (expression.isEmpty()) {
                support.messenger.send(sender, "ledger.not-found", unparsed("material", id));
                return;
            }
            support.messenger.send(sender, "ledger.view", unparsed("material", id),
                    support.value(support.prices.value(id).orElseThrow()), unparsed("expression", expression.get().text()));
        }));
    }
}
