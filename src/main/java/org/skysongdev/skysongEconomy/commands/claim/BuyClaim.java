package org.skysongdev.skysongEconomy.commands.claim;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.claims.ClaimView;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * /se claim buy [claim]: shows what buying the Claim costs (the one named, or the one the player
 * stands in) and buys it when they confirm. The same as the Buy button on a Claim sign.
 */
final class BuyClaim extends ClaimSubCommand {
    BuyClaim(ClaimCommandSupport support) {
        super(support, "claim", "buy", "[claim]", 0, ClaimCommand.BUY);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            support.messenger.send(sender, "command.players-only");
            return;
        }
        if (support.buying == null) {
            support.messenger.send(player, "claims.buy.menus-off");
            return;
        }
        Optional<String> id = args.length > 0 ? Optional.of(args[0].toLowerCase(Locale.ROOT))
                : support.claimAt.apply(player.getLocation()).map(ClaimView::id);
        if (id.isEmpty()) {
            support.messenger.send(player, "claims.buy.not-here");
            return;
        }
        support.buying.open(player, id.get());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.listedIds(args[0]) : List.of();
    }
}
