package org.skysongdev.skysongEconomy.commands.claim;

import org.skysongdev.skysongEconomy.claims.ClaimGateway;
import org.skysongdev.skysongEconomy.claims.ClaimSales;
import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;

/**
 * /se claim: pricing and selling SkysongClaims Claims. The group itself needs no permission, since
 * players will buy and resell through it; each staff command needs {@link #ADMIN}.
 */
public final class ClaimCommand extends CommandGroup {
    /** Staff permission: prices and server sales. */
    public static final String ADMIN = "skysongeconomy.claims.admin";

    public ClaimCommand(ClaimSales sales, ClaimGateway claims, Wallet wallet, Messenger messenger) {
        super("claim", null, messenger);
        ClaimCommandSupport support = new ClaimCommandSupport(sales, claims, wallet, messenger);
        register(new CommandGroup("price", ADMIN, messenger)
                .register(new SetClaimPrice(support))
                .register(new ClearClaimPrice(support))
                .register(new ViewClaimPrice(support)));
        register(new SellClaim(support));
        register(new UnsellClaim(support));
        register(new ListClaimSales(support));
    }
}
