package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.piers.Pier;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.gui.ConfirmMenu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.PagedMenu;
import org.skysongdev.skysongEconomy.gui.layout.BorderedLayout;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.trade.DeliveryTime;
import org.skysongdev.skysongEconomy.trade.Durations;
import org.skysongdev.skysongEconomy.trade.ImportOffer;
import org.skysongdev.skysongEconomy.trade.ImportQuote;
import org.skysongdev.skysongEconomy.trade.Ledger;
import org.skysongdev.skysongEconomy.trade.delivery.Delivery;
import org.skysongdev.skysongEconomy.trade.delivery.OrderCancelledException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Every import of the business's selected ledgers, priced for the delivery time chosen in the
 * right column. Clicking an import asks for confirmation with its contents, delivery time,
 * price and modifiers, and, whenever there are piers, a button to choose its receiving pier,
 * which must be chosen before it can be ordered. Then it is ordered.
 */
final class ImportsMenu extends PagedMenu<ImportsMenu.Choice> {
    /** One import, with the ledger it comes from. */
    record Choice(Ledger ledger, ImportOffer offer) {
    }

    private final ImportScreens screens;
    private final BusinessMenuContext context;
    private final List<DeliveryTime> times;
    private DeliveryTime time;

    ImportsMenu(ImportScreens screens, BusinessMenuContext context) {
        super(screens.menus.theme(), screens.menus.theme().title("business.imports.menu.title",
                Placeholder.unparsed("business", context.business().displayName())));
        this.screens = screens;
        this.context = context;
        List<DeliveryTime> all = List.copyOf(screens.settings.deliveryTimes().values());
        this.times = all.subList(0, Math.min(all.size(), BorderedLayout.rightColumn().size()));
        this.time = times.get(0);
    }

    @Override
    protected List<Choice> items() {
        List<Choice> choices = new ArrayList<>();
        for (Ledger ledger : screens.selections.selected(context.business())) {
            ledger.imports().values().forEach(offer -> choices.add(new Choice(ledger, offer)));
        }
        return choices;
    }

    @Override
    protected MenuButton button(Choice choice) {
        ImportOffer offer = choice.offer();
        List<Component> lore = new ArrayList<>();
        lore.add(screens.messenger.render("business.imports.menu.from", LedgerScreens.name(choice.ledger())));
        lore.addAll(offer.lore().stream().map(screens.messenger::renderText).toList());
        lore.add(Component.empty());
        lore.addAll(screens.contents(offer.contents()));
        lore.add(Component.empty());
        Optional<ImportQuote> quote = Optional.empty();
        if (offer.price(time.id()).isEmpty()) {
            lore.add(screens.messenger.render("business.imports.menu.not-offered", Placeholder.parsed("time", time.displayName())));
        } else {
            try {
                quote = Optional.of(screens.pricing.quote(context.business(), choice.ledger(), offer, time));
                lore.addAll(screens.price(quote.get().price()));
            } catch (PriceException broken) {
                lore.add(screens.messenger.render("business.imports.menu.no-price"));
            }
        }
        lore.add(Component.empty());
        boolean slotFree = screens.deliveries.freeSlots(context.business()) > 0;
        if (quote.isPresent()) {
            lore.add(screens.messenger.render(slotFree ? "business.imports.menu.click" : "business.imports.menu.no-free-slot"));
        }
        ItemStack icon = theme.item(LedgerScreens.material(offer.icon()), screens.messenger.renderText(offer.displayName()), lore);
        if (quote.isEmpty() || !slotFree) {
            return MenuButton.display(icon);
        }
        ImportQuote ordered = quote.get();
        return MenuButton.of(icon, click -> confirm(click.player(), ordered, Optional.empty()));
    }

    @Override
    protected @Nullable MenuButton header() {
        Business business = context.business();
        return MenuButton.display(theme.item(Material.CHEST_MINECART, "business.imports.menu.header.name",
                "business.imports.menu.header.lore",
                Placeholder.unparsed("free", String.valueOf(screens.deliveries.freeSlots(business))),
                Placeholder.unparsed("max", String.valueOf(screens.settings.limits(business).imports())),
                Placeholder.unparsed("ledgers", String.valueOf(screens.selections.selected(business).size()))));
    }

    /** One button per delivery time; the chosen one is green. */
    @Override
    protected void drawSide() {
        List<Integer> slots = BorderedLayout.rightColumn();
        for (int i = 0; i < times.size(); i++) {
            DeliveryTime option = times.get(i);
            TagResolver placeholders = TagResolver.resolver(Placeholder.parsed("time", option.displayName()),
                    Placeholder.unparsed("duration", Durations.format(option.duration())));
            if (option.equals(time)) {
                set(slots.get(i), MenuButton.display(theme.item(Material.LIME_DYE, "business.imports.time.selected.name",
                        "business.imports.time.selected.lore", placeholders)));
            } else {
                set(slots.get(i), MenuButton.of(theme.item(Material.CLOCK, "business.imports.time.other.name",
                        "business.imports.time.other.lore", placeholders), click -> {
                    time = option;
                    redraw();
                }));
            }
        }
    }

    /**
     * Asks to confirm the order, with a button to choose its receiving pier whenever there are piers.
     *
     * @param pierId the pier chosen so far, if any
     */
    private void confirm(Player player, ImportQuote listed, Optional<String> pierId) {
        Optional<Pier> pier = pierId.flatMap(screens.piers::find).filter(Pier::open);
        boolean needsPier = screens.deliveries.needsPier();
        ImportQuote quote;
        try {
            // Priced again for the chosen pier, so pier:<id> modifiers show.
            quote = screens.pricing.quote(context.business(), listed.ledger(), listed.offer(), listed.deliveryTime(),
                    pier.map(Pier::id).orElse(null));
        } catch (PriceException broken) {
            screens.messenger.send(player, broken);
            open(player);
            return;
        }
        List<Component> lore = new ArrayList<>(screens.contents(quote.offer().contents()));
        lore.add(Component.empty());
        lore.add(screens.messenger.render("business.imports.confirm.time", Placeholder.parsed("time", quote.deliveryTime().displayName()),
                Placeholder.unparsed("duration", Durations.format(quote.deliveryTime().duration()))));
        if (needsPier) {
            lore.add(pier.map(chosen -> screens.messenger.render("business.imports.confirm.pier", pierName(chosen)))
                    .orElseGet(() -> screens.messenger.render("business.imports.confirm.no-pier")));
        }
        lore.addAll(screens.price(quote.price()));
        lore.add(Component.empty());
        lore.add(screens.messenger.render("business.imports.confirm.final"));
        ConfirmMenu menu = new ConfirmMenu(theme, theme.title("business.imports.confirm.title"),
                theme.item(LedgerScreens.material(quote.offer().icon()), screens.messenger.renderText(quote.offer().displayName()), lore),
                click -> order(click.player(), quote, pier),
                click -> open(click.player()));
        if (needsPier) {
            menu.withExtra(pierButton(quote, pier));
        }
        menu.open(player);
    }

    /** The confirmation's pier button: shows the chosen pier, and opens the list of piers. */
    private MenuButton pierButton(ImportQuote quote, Optional<Pier> pier) {
        Optional<String> chosen = pier.map(Pier::id);
        ItemStack icon = pier.map(chosenPier -> theme.item(LedgerScreens.material(chosenPier.details().icon()),
                        "business.imports.pier.chosen.name", "business.imports.pier.chosen.lore", pierName(chosenPier)))
                .orElseGet(() -> theme.item(Material.BARRIER, "business.imports.pier.none.name",
                        "business.imports.pier.none.lore"));
        return MenuButton.of(icon, click -> new PierMenu(screens, chosen,
                (player, picked) -> confirm(player, quote, Optional.of(picked.id())),
                player -> confirm(player, quote, chosen)).open(click.player()));
    }

    private void order(Player player, ImportQuote quote, Optional<Pier> pier) {
        Business business = context.business();
        if (screens.deliveries.needsPier() && pier.isEmpty()) {
            screens.messenger.send(player, "business.imports.choose-pier");
            confirm(player, quote, Optional.empty());
            return;
        }
        Optional<CharacterRef> character = screens.menus.context(player, business).character();
        if (character.isEmpty()) {
            screens.messenger.send(player, "business.imports.no-character");
            return;
        }
        try {
            Delivery delivery = screens.deliveries.order(business, character.get(), quote.ledger(), quote.offer(),
                    quote.deliveryTime(), pier.map(Pier::id).orElse(null));
            TagResolver details = TagResolver.resolver(Placeholder.parsed("import", quote.offer().displayName()),
                    Placeholder.unparsed("price", screens.wallet.format(delivery.price())),
                    Placeholder.parsed("time", quote.deliveryTime().displayName()),
                    Placeholder.unparsed("duration", Durations.format(quote.deliveryTime().duration())));
            if (pier.isPresent()) {
                screens.messenger.send(player, "business.imports.ordered-pier", details, pierName(pier.get()));
            } else {
                screens.messenger.send(player, "business.imports.ordered", details);
            }
        } catch (OrderCancelledException cancelled) {
            cancelled.reason().ifPresentOrElse(
                    reason -> screens.messenger.send(player, "business.imports.order-cancelled-reason",
                            Placeholder.parsed("reason", reason)),
                    () -> screens.messenger.send(player, cancelled));
        } catch (RefusedException refusal) {
            screens.messenger.send(player, refusal);
        }
        screens.open(player, business);
    }

    /** @return {@code <pier>}: the pier's MiniMessage name */
    private static TagResolver pierName(Pier pier) {
        return Placeholder.parsed("pier", pier.details().displayName());
    }

    @Override
    protected @Nullable MenuButton back() {
        return theme.back(click -> screens.menus.openMain(click.player(), context.business()));
    }
}
