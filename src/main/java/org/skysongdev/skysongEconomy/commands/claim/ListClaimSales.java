package org.skysongdev.skysongEconomy.commands.claim;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.claims.ClaimQuote;
import org.skysongdev.skysongEconomy.claims.ClaimView;
import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** /se claim list: every Claim SkysongEconomy has on sale, with what a buyer pays for it now. */
final class ListClaimSales extends ClaimSubCommand {
    ListClaimSales(ClaimCommandSupport support) {
        super(support, "claim", "list", "", 0, ClaimCommand.ADMIN);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        List<ClaimView> listed = support.sales.listed().stream().sorted(Comparator.comparing(ClaimView::id)).toList();
        if (listed.isEmpty()) {
            support.messenger.send(sender, "claims.list-empty");
            return;
        }
        support.messenger.send(sender, "claims.list-header", Placeholder.unparsed("count", Integer.toString(listed.size())));
        for (ClaimView claim : listed) {
            Optional<ClaimQuote> quote;
            try {
                quote = Optional.of(support.pricing.quote(claim.id()));
            } catch (RefusedException e) {
                quote = Optional.empty();
            }
            if (quote.isPresent()) {
                support.messenger.send(sender, "claims.list-entry", ClaimCommandSupport.claim(claim.id()),
                        Placeholder.unparsed("type", claim.typeId()), support.price(quote.get().total()),
                        Placeholder.unparsed("asking", support.wallet.format(quote.get().askingPrice())));
            } else {
                support.messenger.send(sender, "claims.list-entry-broken", ClaimCommandSupport.claim(claim.id()),
                        Placeholder.unparsed("type", claim.typeId()));
            }
        }
    }
}
