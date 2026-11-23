package org.skysongdev.skysongEconomy.commands.claim;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.claims.ClaimGateway;
import org.skysongdev.skysongEconomy.claims.ClaimPricing;
import org.skysongdev.skysongEconomy.claims.ClaimSales;
import org.skysongdev.skysongEconomy.claims.ClaimView;
import org.skysongdev.skysongEconomy.gui.claim.ClaimBuyScreens;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** What the /se claim commands share: the services, and turning amounts and Claims into message placeholders. */
final class ClaimCommandSupport {
    final ClaimSales sales;
    final ClaimPricing pricing;
    final ClaimGateway claims;
    final Wallet wallet;
    final Messenger messenger;
    /** The buying confirmation, or {@code null} when menus are off because businesses are disabled. */
    final @Nullable ClaimBuyScreens buying;
    final Function<Location, Optional<ClaimView>> claimAt;

    ClaimCommandSupport(ClaimSales sales, ClaimPricing pricing, ClaimGateway claims, Wallet wallet, Messenger messenger,
                        @Nullable ClaimBuyScreens buying, Function<Location, Optional<ClaimView>> claimAt) {
        this.sales = sales;
        this.pricing = pricing;
        this.claims = claims;
        this.wallet = wallet;
        this.messenger = messenger;
        this.buying = buying;
        this.claimAt = claimAt;
    }

    /** @return {@code <claim>}: the Claim's id */
    static TagResolver claim(String claimId) {
        return Placeholder.unparsed("claim", claimId);
    }

    /** @return {@code <price>}: the amount as players read it, such as {@code 2,000 Pouches} */
    TagResolver price(BigDecimal amount) {
        return Placeholder.unparsed("price", wallet.format(amount));
    }

    /** Shows why something was refused. */
    void refuse(CommandSender sender, RefusedException refusal) {
        messenger.send(sender, refusal);
    }

    /** @return the words from {@code from} on, joined by spaces, such as a price expression typed with spaces */
    static String join(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    /** @return the ids of the Claims that start with what was typed, for tab completion */
    List<String> claimIds(String typed) {
        String lower = typed.toLowerCase(Locale.ROOT);
        return claims.all().stream().map(ClaimView::id).filter(id -> id.startsWith(lower)).sorted().toList();
    }

    /** @return the ids of the Claims SkysongEconomy has on sale that start with what was typed */
    List<String> listedIds(String typed) {
        String lower = typed.toLowerCase(Locale.ROOT);
        return sales.listed().stream().map(ClaimView::id).filter(id -> id.startsWith(lower)).sorted().toList();
    }
}
