package org.skysongdev.skysongEconomy.commands.claim;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.math.BigDecimal;
import java.util.List;

/** /se claim price set &lt;claim&gt; &lt;price&gt;: staff set a Claim's asking price. A Claim on sale stays on sale at it. */
final class SetClaimPrice extends ClaimSubCommand {
    SetClaimPrice(ClaimCommandSupport support) {
        super(support, "claim price", "set", "<claim> <price>", 2, null);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String expression = ClaimCommandSupport.join(args, 1);
        BigDecimal value = support.sales.setPrice(args[0], expression, sender.getName());
        String id = support.sales.prices().find(args[0]).orElseThrow().claimId();
        support.messenger.send(sender, "claims.price-set", ClaimCommandSupport.claim(id), support.price(value),
                Placeholder.unparsed("expression", expression));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.claimIds(args[0]) : List.of();
    }
}
