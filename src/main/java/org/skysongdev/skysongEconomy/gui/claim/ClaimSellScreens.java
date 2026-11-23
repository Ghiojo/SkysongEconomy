package org.skysongdev.skysongEconomy.gui.claim;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.claims.ClaimGateway;
import org.skysongdev.skysongEconomy.claims.ClaimListing;
import org.skysongdev.skysongEconomy.claims.ClaimSales;
import org.skysongdev.skysongEconomy.claims.ClaimView;
import org.skysongdev.skysongEconomy.gui.ChatPrompts;
import org.skysongdev.skysongEconomy.gui.ConfirmMenu;
import org.skysongdev.skysongEconomy.gui.MenuTheme;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Owners selling their Claim from its sign: the Sell button asks for a price in chat, shows what
 * they'd ask and that it is shared between the owners, and puts it on sale when they confirm. On a
 * Claim already on sale, the same button takes it off sale. Main thread only.
 */
public final class ClaimSellScreens {
    private static final Material SELL_ICON = Material.GOLD_INGOT;
    private static final Material UNSELL_ICON = Material.BARRIER;

    private final ClaimSales sales;
    private final ClaimGateway claims;
    private final CharacterDirectory characters;
    private final Wallet wallet;
    private final MenuTheme theme;
    private final ChatPrompts prompts;
    private final Messenger messenger;
    private final BiConsumer<ClaimListing, BigDecimal> listed;

    /** @param listed told when an owner put a Claim on sale here, with what it sells for, to tell the other owners */
    public ClaimSellScreens(ClaimSales sales, ClaimGateway claims, CharacterDirectory characters, Wallet wallet,
                            MenuTheme theme, ChatPrompts prompts, Messenger messenger,
                            BiConsumer<ClaimListing, BigDecimal> listed) {
        this.sales = sales;
        this.claims = claims;
        this.characters = characters;
        this.wallet = wallet;
        this.theme = theme;
        this.prompts = prompts;
        this.messenger = messenger;
        this.listed = listed;
    }

    /**
     * @return for an owner of the Claim: Sell when it isn't on sale, Take off sale when SkysongEconomy
     *         sells it; empty for anyone else, Free Claims, and Claims another plugin sells
     */
    public Optional<ItemStack> buttonIcon(Player viewer, String claimId) {
        Optional<ClaimView> claim = ownedClaim(viewer, claimId).filter(found -> !found.free());
        if (claim.isEmpty()) {
            return Optional.empty();
        }
        if (sales.isOurs(claim.get())) {
            String price;
            try {
                price = sales.prices().value(claim.get().id()).map(wallet::format).orElse("?");
            } catch (RefusedException broken) {
                price = "?";
            }
            return Optional.of(theme.item(UNSELL_ICON, messenger.render("claims.sell.unsell-button-name"),
                    List.of(messenger.render("claims.sell.unsell-button-lore", Placeholder.unparsed("price", price)))));
        }
        if (claim.get().isForSale()) {
            return Optional.empty();
        }
        return Optional.of(theme.item(SELL_ICON, messenger.render("claims.sell.button-name"),
                List.of(messenger.render("claims.sell.button-lore"))));
    }

    /** The button was clicked: asks for a price, or takes the Claim off sale. */
    public void click(Player player, String claimId) {
        Optional<CharacterRef> owner = characters.active(player.getUniqueId());
        Optional<ClaimView> claim = ownedClaim(player, claimId);
        if (owner.isEmpty() || claim.isEmpty()) {
            messenger.send(player, "claims.sell.not-owner", Placeholder.unparsed("claim", claimId));
            return;
        }
        if (sales.isOurs(claim.get())) {
            run(player, () -> {
                sales.unsellByOwner(owner.get(), claimId);
                messenger.send(player, "claims.sell.unlisted", Placeholder.unparsed("claim", claimId));
            });
            return;
        }
        prompts.ask(player, "claims.sell.prompt", answer -> confirm(player, owner.get(), claim.get(), answer));
    }

    private void confirm(Player player, CharacterRef owner, ClaimView claim, String price) {
        run(player, () -> {
            BigDecimal value = sales.prices().check(price);
            List<Component> lore = List.of(
                    messenger.render("claims.sell.confirm.price", price(value)),
                    messenger.render("claims.sell.confirm.split",
                            Placeholder.unparsed("count", String.valueOf(claim.owners().size()))),
                    messenger.render("claims.sell.confirm.modifiers"));
            ItemStack summary = theme.item(SELL_ICON, Component.text(claim.displayName()), lore);
            new ConfirmMenu(theme, theme.title("claims.sell.confirm.title", Placeholder.unparsed("name", claim.displayName())),
                    summary, click -> list(click.player(), owner, claim.id(), price),
                    click -> click.player().closeInventory()).open(player);
        });
    }

    private void list(Player player, CharacterRef owner, String claimId, String price) {
        run(player, () -> {
            BigDecimal value = sales.listByOwner(owner, claimId, price);
            messenger.send(player, "claims.sell.listed", Placeholder.unparsed("claim", claimId), price(value));
            sales.listings().find(claimId).ifPresent(listing -> listed.accept(listing, value));
        });
    }

    /** @return the Claim, when the viewer's active character owns it */
    private Optional<ClaimView> ownedClaim(Player viewer, String claimId) {
        Optional<CharacterRef> character = characters.active(viewer.getUniqueId());
        return character.flatMap(owner -> claims.find(claimId).filter(claim -> claim.isOwnedBy(owner)));
    }

    /** Runs a step, showing the player why it was refused if it was. */
    private void run(Player player, Runnable step) {
        try {
            step.run();
        } catch (RefusedException refusal) {
            messenger.send(player, refusal);
        }
    }

    private TagResolver price(BigDecimal amount) {
        return Placeholder.unparsed("price", wallet.format(amount));
    }
}
