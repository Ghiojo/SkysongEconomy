package org.skysongdev.skysongEconomy.gui.claim;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.api.claims.ClaimSale;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.claims.ClaimException;
import org.skysongdev.skysongEconomy.claims.ClaimGateway;
import org.skysongdev.skysongEconomy.claims.ClaimPurchases;
import org.skysongdev.skysongEconomy.claims.ClaimQuote;
import org.skysongdev.skysongEconomy.claims.ClaimView;
import org.skysongdev.skysongEconomy.gui.ConfirmMenu;
import org.skysongdev.skysongEconomy.gui.MenuTheme;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Buying a Claim from a player's side: the confirmation that shows what they pay and why, and the
 * Buy button's icon in a Claim sign's menu. Both {@code /se claim buy} and the button open
 * {@link #open}. Main thread only.
 */
public final class ClaimBuyScreens {
    private static final Material BUTTON_ICON = Material.EMERALD;
    private static final Material SUMMARY_ICON = Material.OAK_DOOR;

    private final ClaimPurchases purchases;
    private final ClaimGateway claims;
    private final CharacterDirectory characters;
    private final Wallet wallet;
    private final MenuTheme theme;
    private final Messenger messenger;

    public ClaimBuyScreens(ClaimPurchases purchases, ClaimGateway claims, CharacterDirectory characters, Wallet wallet,
                           MenuTheme theme, Messenger messenger) {
        this.purchases = purchases;
        this.claims = claims;
        this.characters = characters;
        this.wallet = wallet;
        this.theme = theme;
        this.messenger = messenger;
    }

    /**
     * Shows the player what buying the Claim costs their character, and buys it when they confirm.
     * When they can't buy it, says why instead.
     */
    public void open(Player player, String claimId) {
        Optional<CharacterRef> character = characters.active(player.getUniqueId());
        if (character.isEmpty()) {
            messenger.send(player, "claims.buy.no-character");
            return;
        }
        try {
            ClaimQuote quote = purchases.offer(character.get(), claimId);
            ClaimView claim = claims.find(quote.claimId()).orElseThrow();
            new ConfirmMenu(theme, theme.title("claims.buy.confirm.title", name(claim)), summary(player, claim, quote,
                    character.get()), click -> buy(click.player(), character.get(), quote),
                    click -> click.player().closeInventory()).open(player);
        } catch (RefusedException refusal) {
            messenger.send(player, refusal);
        }
    }

    /** @return the Buy button for a Claim SkysongEconomy has on sale, showing what it costs; empty otherwise */
    public Optional<ItemStack> buttonIcon(String claimId) {
        try {
            return claims.find(claimId).filter(claim -> purchases.isForSale(claim)).map(claim ->
                    theme.item(BUTTON_ICON, messenger.render("claims.buy.button-name"), List.of(messenger.render(
                            "claims.buy.button-lore", price(purchases.price(claim.id()))))));
        } catch (RefusedException e) {
            // Its price can't be worked out: no button, the confirmation would only refuse.
            return Optional.empty();
        }
    }

    private void buy(Player player, CharacterRef shown, ClaimQuote quote) {
        // The confirmation said who will own it; if the player switched character meanwhile, show it again.
        if (!characters.active(player.getUniqueId()).equals(Optional.of(shown))) {
            open(player, quote.claimId());
            return;
        }
        try {
            ClaimSale sale = purchases.buy(shown, quote.claimId(), quote.total());
            ClaimView claim = claims.find(sale.claimId()).orElseThrow();
            messenger.send(player, "claims.buy.bought", name(claim), price(sale.paid()),
                    Placeholder.unparsed("character", shown.profile()));
        } catch (ClaimException refusal) {
            messenger.send(player, refusal);
            if (refusal.messageKey().equals("claims.buy.price-changed")) {
                open(player, quote.claimId());
            }
        } catch (RefusedException refusal) {
            messenger.send(player, refusal);
        }
    }

    private ItemStack summary(Player player, ClaimView claim, ClaimQuote quote, CharacterRef character) {
        List<Component> lore = new ArrayList<>();
        lore.add(messenger.render("claims.buy.confirm.type", Placeholder.unparsed("type", claim.typeId())));
        lore.add(messenger.render("claims.buy.confirm.seller-server"));
        lore.add(Component.empty());
        lore.add(messenger.render("claims.buy.confirm.asking", price(quote.askingPrice())));
        quote.shares().forEach(share -> lore.add(messenger.render("claims.buy.confirm.share",
                Placeholder.parsed("name", share.modifier().displayName()),
                Placeholder.unparsed("share", wallet.format(share.amount())))));
        lore.add(messenger.render("claims.buy.confirm.total", price(quote.total())));
        lore.add(Component.empty());
        lore.add(messenger.render("claims.buy.confirm.balance",
                Placeholder.unparsed("balance", wallet.format(wallet.balance(player.getUniqueId())))));
        lore.add(messenger.render("claims.buy.confirm.owner", Placeholder.unparsed("character", character.profile())));
        return theme.item(SUMMARY_ICON, Component.text(claim.displayName()), lore);
    }

    private static TagResolver name(ClaimView claim) {
        return Placeholder.unparsed("name", claim.displayName());
    }

    private TagResolver price(BigDecimal amount) {
        return Placeholder.unparsed("price", wallet.format(amount));
    }
}
