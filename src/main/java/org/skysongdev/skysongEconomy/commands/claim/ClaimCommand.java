package org.skysongdev.skysongEconomy.commands.claim;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.claims.ClaimGateway;
import org.skysongdev.skysongEconomy.claims.ClaimListing;
import org.skysongdev.skysongEconomy.claims.ClaimPricing;
import org.skysongdev.skysongEconomy.claims.ClaimSales;
import org.skysongdev.skysongEconomy.claims.ClaimView;
import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.gui.claim.ClaimBuyScreens;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * /se claim: pricing, selling and buying SkysongClaims Claims. The group itself needs no
 * permission, since players buy and resell through it; staff-only commands need {@link #ADMIN}.
 */
public final class ClaimCommand extends CommandGroup {
    /** Staff permission: prices and server sales. */
    public static final String ADMIN = "skysongeconomy.claims.admin";
    /** Player permission: buying Claims. */
    public static final String BUY = "skysongeconomy.claims.buy";
    /** Player permission: putting your own Claim on sale and taking it off. */
    public static final String SELL = "skysongeconomy.claims.sell";

    /**
     * What the commands use.
     *
     * @param characters players' characters, which own Claims
     * @param buying     the buying confirmation, or {@code null} when menus are off because businesses are disabled
     * @param claimAt    finds the Claim at a spot, for {@code /se claim buy} without a Claim
     * @param listed     told when an owner put a Claim on sale, with what it sells for, to tell the other owners
     */
    public record Services(ClaimSales sales, ClaimPricing pricing, ClaimGateway claims, Wallet wallet,
                           CharacterDirectory characters, Messenger messenger, @Nullable ClaimBuyScreens buying,
                           Function<Location, Optional<ClaimView>> claimAt, BiConsumer<ClaimListing, BigDecimal> listed) {
    }

    public ClaimCommand(Services services) {
        super("claim", null, services.messenger());
        ClaimCommandSupport support = new ClaimCommandSupport(services);
        Messenger messenger = services.messenger();
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
