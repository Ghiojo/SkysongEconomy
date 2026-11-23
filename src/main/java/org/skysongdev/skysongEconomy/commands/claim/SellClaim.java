package org.skysongdev.skysongEconomy.commands.claim;

import org.bukkit.command.CommandSender;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * /se claim sell &lt;claim&gt; [price]: staff put an unowned Claim on sale for the server, at the
 * price given (which is saved) or at its saved price.
 */
final class SellClaim extends ClaimSubCommand {
    SellClaim(ClaimCommandSupport support) {
        super(support, "claim", "sell", "<claim> [price]", 1, ClaimCommand.ADMIN);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String id = args[0].toLowerCase(Locale.ROOT);
        String price = args.length > 1 ? ClaimCommandSupport.join(args, 1) : null;
        BigDecimal value = support.sales.sell(id, price, sender.getName());
        support.messenger.send(sender, "claims.listed", ClaimCommandSupport.claim(id), support.price(value));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.claimIds(args[0]) : List.of();
    }
}
