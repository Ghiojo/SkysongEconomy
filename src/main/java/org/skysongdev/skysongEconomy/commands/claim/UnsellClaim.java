package org.skysongdev.skysongEconomy.commands.claim;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * /se claim unsell &lt;claim&gt;: takes a Claim SkysongEconomy is selling off sale. Any owner can,
 * whoever put it on sale; staff can for any Claim. Its price stays saved.
 */
final class UnsellClaim extends ClaimSubCommand {
    UnsellClaim(ClaimCommandSupport support) {
        super(support, "claim", "unsell", "<claim>", 1, ClaimCommand.SELL);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String id = args[0].toLowerCase(Locale.ROOT);
        Optional<CharacterRef> character = support.character(sender);
        if (ClaimCommandSupport.isStaff(sender)) {
            support.sales.unsell(id);
            support.messenger.send(sender, "claims.unlisted", ClaimCommandSupport.claim(id));
        } else if (character.isPresent()) {
            support.sales.unsellByOwner(character.get(), id);
            support.messenger.send(sender, "claims.sell.unlisted", ClaimCommandSupport.claim(id));
        } else {
            support.messenger.send(sender, "claims.sell.not-owner", ClaimCommandSupport.claim(id));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        if (ClaimCommandSupport.isStaff(sender)) {
            return support.listedIds(args[0]);
        }
        List<String> listed = support.listedIds(args[0]);
        return support.ownedIds(sender, args[0]).stream().filter(listed::contains).toList();
    }
}
