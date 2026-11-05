package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.vault.BusinessVaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business vault &lt;id&gt; [collect]: the owner or staff see what the vault holds; with
 * {@code collect}, it is paid to the owner. Staff collecting still pays the owner.
 */
final class VaultCommand extends BusinessSubCommand {
    private final BusinessVaults vaults;

    VaultCommand(BusinessCommandSupport support, BusinessVaults vaults) {
        super(support, "vault", "<id> [collect]", 1, null);
        this.vaults = vaults;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        support.requireManager(sender, business);
        if (args.length > 1 && args[1].equalsIgnoreCase("collect")) {
            BigDecimal paid = vaults.collect(business);
            support.messenger.send(sender, "business.vault.collected", unparsed("amount", vaults.format(paid)),
                    unparsed("owner", support.describe(business.owner())), unparsed("business", business.displayName()));
            return;
        }
        support.messenger.send(sender, "business.vault.balance",
                unparsed("amount", vaults.format(vaults.balance(business))), unparsed("business", business.displayName()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.businessIds(args[0]);
            case 2 -> "collect".startsWith(args[1].toLowerCase(Locale.ROOT)) ? List.of("collect") : List.of();
            default -> List.of();
        };
    }
}
