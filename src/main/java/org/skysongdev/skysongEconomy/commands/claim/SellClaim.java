package org.skysongdev.skysongEconomy.commands.claim;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.claims.ClaimView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * /se claim sell &lt;claim&gt; [price]:
 * <ul>
 *     <li>an owner puts their whole Claim on sale at the price they ask, which becomes its price;
 *         the other owners are told;</li>
 *     <li>staff put an unowned Claim on sale for the server, at the price given (which is saved) or
 *         its saved price.</li>
 * </ul>
 */
final class SellClaim extends ClaimSubCommand {
    SellClaim(ClaimCommandSupport support) {
        super(support, "claim", "sell", "<claim> [price]", 1, ClaimCommand.SELL);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String id = args[0].toLowerCase(Locale.ROOT);
        String price = args.length > 1 ? ClaimCommandSupport.join(args, 1) : null;
        Optional<CharacterRef> owner = support.character(sender)
                .filter(character -> support.claims.find(id).filter(claim -> claim.isOwnedBy(character)).isPresent());
        if (owner.isPresent()) {
            sellOwn(sender, owner.get(), id, price);
        } else if (ClaimCommandSupport.isStaff(sender)) {
            BigDecimal value = support.sales.sell(id, price, sender.getName());
            support.messenger.send(sender, "claims.listed", ClaimCommandSupport.claim(id), support.price(value));
        } else {
            support.messenger.send(sender, "claims.sell.not-owner", ClaimCommandSupport.claim(id));
        }
    }

    private void sellOwn(CommandSender sender, CharacterRef owner, String id, String price) {
        if (price == null) {
            support.messenger.send(sender, "claims.sell.price-needed", ClaimCommandSupport.claim(id));
            return;
        }
        BigDecimal value = support.sales.listByOwner(owner, id, price);
        support.messenger.send(sender, "claims.sell.listed", ClaimCommandSupport.claim(id), support.price(value));
        support.sales.listings().find(id).ifPresent(listing -> support.listed.accept(listing, value));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        return ClaimCommandSupport.isStaff(sender) ? support.claimIds(args[0]) : support.ownedIds(sender, args[0]);
    }
}
