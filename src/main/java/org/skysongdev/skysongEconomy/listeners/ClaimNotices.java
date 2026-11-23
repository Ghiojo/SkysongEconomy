package org.skysongdev.skysongEconomy.listeners;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skysongdev.skysongEconomy.api.claims.ClaimSale;
import org.skysongdev.skysongEconomy.api.claims.Seller;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.claims.ClaimGateway;
import org.skysongdev.skysongEconomy.claims.ClaimListing;
import org.skysongdev.skysongEconomy.claims.ClaimPayouts;
import org.skysongdev.skysongEconomy.claims.ClaimSales;
import org.skysongdev.skysongEconomy.claims.ClaimView;
import org.skysongdev.skysongEconomy.claims.PendingPayout;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Tells Claim owners what happens to their sales: co-owners when one of them puts the Claim on sale
 * (at once if online, on joining otherwise, for as long as it stays on sale), sellers what they
 * received, and pays sellers' waiting shares when they join.
 */
public final class ClaimNotices implements Listener {
    private final Server server;
    private final ClaimGateway claims;
    private final ClaimSales sales;
    private final ClaimPayouts payouts;
    private final Wallet wallet;
    private final Messenger messenger;

    public ClaimNotices(Server server, ClaimGateway claims, ClaimSales sales, ClaimPayouts payouts, Wallet wallet,
                        Messenger messenger) {
        this.server = server;
        this.claims = claims;
        this.sales = sales;
        this.payouts = payouts;
        this.wallet = wallet;
        this.messenger = messenger;
    }

    /** An owner put the Claim on sale: its other owners who are online are told. */
    public void listed(ClaimListing listing, BigDecimal price) {
        claims.find(listing.claimId()).ifPresent(claim -> coOwners(claim, listing).stream()
                .map(server::getPlayer).filter(Objects::nonNull)
                .forEach(player -> tellListed(player, listing, wallet.format(price))));
    }

    /** A Claim sold: its sellers who are online are told what they received. */
    public void sold(ClaimSale sale) {
        sale.seller().map(Seller::payouts).ifPresent(shares -> shares.forEach((account, amount) -> {
            Player player = server.getPlayer(account);
            if (player == null) {
                return;
            }
            boolean waiting = payouts.owed(account).stream().anyMatch(payout -> payout.saleId() == sale.id());
            messenger.send(player, waiting ? "claims.sell.sold-later" : "claims.sell.sold",
                    Placeholder.unparsed("claim", sale.claimId()), Placeholder.unparsed("buyer", sale.buyer().profile()),
                    Placeholder.unparsed("price", wallet.format(amount)));
        }));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID account = player.getUniqueId();
        for (PendingPayout paid : payouts.payOut(account)) {
            messenger.send(player, "claims.payout.paid", Placeholder.unparsed("claim", paid.claimId()),
                    Placeholder.unparsed("price", wallet.format(paid.amount())));
        }
        for (PendingPayout waiting : payouts.owed(account)) {
            messenger.send(player, "claims.payout.waiting", Placeholder.unparsed("claim", waiting.claimId()),
                    Placeholder.unparsed("price", wallet.format(waiting.amount())));
        }
        for (ClaimListing listing : sales.listings().all()) {
            claims.find(listing.claimId()).filter(claim -> coOwners(claim, listing).contains(account))
                    .ifPresent(claim -> tellListed(player, listing, price(claim.id())));
        }
    }

    /** @return the Claim's asking price as players read it, or {@code ?} when it can't be worked out */
    private String price(String claimId) {
        try {
            return sales.prices().value(claimId).map(wallet::format).orElse("?");
        } catch (RefusedException broken) {
            return "?";
        }
    }

    private void tellListed(Player player, ClaimListing listing, String price) {
        messenger.send(player, "claims.sell.co-owner-listed", Placeholder.unparsed("lister", listing.listedBy().profile()),
                Placeholder.unparsed("claim", listing.claimId()), Placeholder.unparsed("price", price));
    }

    /** @return the accounts owning the Claim, other than the player who listed it */
    private static List<UUID> coOwners(ClaimView claim, ClaimListing listing) {
        return claim.owners().stream().map(CharacterRef::player)
                .filter(account -> !account.equals(listing.listedBy().player())).distinct().toList();
    }
}
