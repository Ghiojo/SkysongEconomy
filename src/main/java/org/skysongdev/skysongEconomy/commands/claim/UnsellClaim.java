package org.skysongdev.skysongEconomy.commands.claim;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

/** /se claim unsell &lt;claim&gt;: staff take a Claim the server is selling off sale. Its price stays saved. */
final class UnsellClaim extends ClaimSubCommand {
    UnsellClaim(ClaimCommandSupport support) {
        super(support, "claim", "unsell", "<claim>", 1, ClaimCommand.ADMIN);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String id = args[0].toLowerCase(Locale.ROOT);
        support.sales.unsell(id);
        support.messenger.send(sender, "claims.unlisted", ClaimCommandSupport.claim(id));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.listedIds(args[0]) : List.of();
    }
}
