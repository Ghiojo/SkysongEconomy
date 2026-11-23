package org.skysongdev.skysongEconomy.commands.claim;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

/** /se claim price clear &lt;claim&gt;: staff remove a Claim's asking price, which also takes it off the server's sale. */
final class ClearClaimPrice extends ClaimSubCommand {
    ClearClaimPrice(ClaimCommandSupport support) {
        super(support, "claim price", "clear", "<claim>", 1, null);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String id = args[0].toLowerCase(Locale.ROOT);
        boolean wasListed = support.sales.clearPrice(id);
        support.messenger.send(sender, wasListed ? "claims.price-cleared-unlisted" : "claims.price-cleared",
                ClaimCommandSupport.claim(id));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String typed = args[0].toLowerCase(Locale.ROOT);
        return support.sales.prices().all().keySet().stream().filter(id -> id.startsWith(typed)).sorted().toList();
    }
}
