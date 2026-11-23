package org.skysongdev.skysongEconomy.commands.claim;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.claims.ClaimGateway;
import org.skysongdev.skysongEconomy.claims.ClaimPricing;
import org.skysongdev.skysongEconomy.claims.ClaimSales;
import org.skysongdev.skysongEconomy.claims.ClaimView;
import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.gui.claim.ClaimBuyScreens;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.util.Optional;
import java.util.function.Function;

/**
 * /se claim: pricing and selling SkysongClaims Claims. The group itself needs no permission, since
 * players will buy and resell through it; each staff command needs {@link #ADMIN}.
 */
public final class ClaimCommand extends CommandGroup {
    /** Staff permission: prices and server sales. */
    public static final String ADMIN = "skysongeconomy.claims.admin";
    /** Player permission: buying Claims. */
    public static final String BUY = "skysongeconomy.claims.buy";

    /**
     * @param buying  the buying confirmation, or {@code null} when menus are off because businesses are disabled
     * @param claimAt finds the Claim at a spot, for {@code /se claim buy} without a Claim
     */
    public ClaimCommand(ClaimSales sales, ClaimPricing pricing, ClaimGateway claims, Wallet wallet, Messenger messenger,
                        @Nullable ClaimBuyScreens buying, Function<Location, Optional<ClaimView>> claimAt) {
        super("claim", null, messenger);
        ClaimCommandSupport support = new ClaimCommandSupport(sales, pricing, claims, wallet, messenger, buying, claimAt);
        register(new CommandGroup("price", ADMIN, messenger)
                .register(new SetClaimPrice(support))
                .register(new ClearClaimPrice(support))
                .register(new ViewClaimPrice(support)));
        register(new SellClaim(support));
        register(new UnsellClaim(support));
        register(new ListClaimSales(support));
        register(new BuyClaim(support));
    }
}
