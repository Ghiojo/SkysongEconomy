package org.skysongdev.skysongEconomy.commands.claim;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.claims.ClaimPrice;
import org.skysongdev.skysongEconomy.claims.ClaimQuote;
import org.skysongdev.skysongEconomy.prices.PriceException;

import java.util.List;
import java.util.Optional;

/**
 * /se claim price view &lt;claim&gt;: a Claim's asking price, the expression behind it and who set
 * it, then each modifier's share and what a buyer pays in total.
 */
final class ViewClaimPrice extends ClaimSubCommand {
    ViewClaimPrice(ClaimCommandSupport support) {
        super(support, "claim price", "view", "<claim>", 1, null);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Optional<ClaimPrice> price = support.sales.prices().find(args[0]);
        if (price.isEmpty()) {
            support.messenger.send(sender, "claims.price-none", ClaimCommandSupport.claim(args[0]));
            return;
        }
        ClaimPrice found = price.get();
        ClaimQuote quote;
        try {
            quote = support.pricing.quote(found.claimId());
        } catch (PriceException e) {
            support.messenger.send(sender, "claims.price-view-broken", ClaimCommandSupport.claim(found.claimId()),
                    Placeholder.unparsed("expression", found.expression().text()),
                    Placeholder.unparsed("by", found.setBy()));
            return;
        }
        support.messenger.send(sender, "claims.price-view", ClaimCommandSupport.claim(found.claimId()),
                support.price(quote.askingPrice()), Placeholder.unparsed("expression", found.expression().text()),
                Placeholder.unparsed("by", found.setBy()));
        for (ClaimQuote.Share share : quote.shares()) {
            support.messenger.send(sender, "claims.price-share", Placeholder.parsed("name", share.modifier().displayName()),
                    Placeholder.unparsed("amount", share.modifier().amount().signed()),
                    Placeholder.unparsed("target", share.modifier().target().key()),
                    Placeholder.unparsed("sign", share.sign()),
                    Placeholder.unparsed("share", support.wallet.format(share.magnitude())));
        }
        if (quote.isModified()) {
            support.messenger.send(sender, "claims.price-total", support.price(quote.total()));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.claimIds(args[0]) : List.of();
    }
}
