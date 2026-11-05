package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.gui.ConfirmMenu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.PagedMenu;
import org.skysongdev.skysongEconomy.gui.layout.BorderedLayout;
import org.skysongdev.skysongEconomy.trade.DeliveryTime;
import org.skysongdev.skysongEconomy.trade.ExportOffer;
import org.skysongdev.skysongEconomy.trade.ImportOffer;
import org.skysongdev.skysongEconomy.trade.Ledger;
import org.skysongdev.skysongEconomy.trade.TradeOffer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * One ledger: its imports, then its exports, with a Select or Remove button in the right column
 * for the owner. Both ask for confirmation, since removing costs the fine.
 */
final class LedgerDetailsMenu extends PagedMenu<TradeOffer> {
    private final LedgerScreens screens;
    private final BusinessMenuContext context;
    private final Ledger ledger;

    LedgerDetailsMenu(LedgerScreens screens, BusinessMenuContext context, Ledger ledger) {
        super(screens.menus.theme(), screens.menus.theme().title("business.ledgers.details.title", LedgerScreens.name(ledger)));
        this.screens = screens;
        this.context = context;
        this.ledger = ledger;
    }

    /** Imports first, then exports. */
    @Override
    protected List<TradeOffer> items() {
        return Stream.<TradeOffer>concat(ledger.imports().values().stream(), ledger.exports().values().stream()).toList();
    }

    @Override
    protected MenuButton button(TradeOffer offer) {
        return switch (offer) {
            case ImportOffer importOffer -> importButton(importOffer);
            case ExportOffer exportOffer -> exportButton(exportOffer);
        };
    }

    private MenuButton importButton(ImportOffer offer) {
        List<Component> lore = new ArrayList<>();
        lore.add(screens.messenger.render("business.ledgers.details.import-tag"));
        lore.addAll(screens.render(offer.lore()));
        String times = offer.prices().keySet().stream()
                .map(id -> screens.settings.deliveryTime(id).map(DeliveryTime::displayName).orElse(id))
                .collect(Collectors.joining("<gray>, "));
        lore.add(Component.empty());
        lore.add(screens.messenger.render("business.ledgers.details.import-times", Placeholder.parsed("times", times)));
        return MenuButton.display(theme.item(LedgerScreens.material(offer.icon()),
                screens.messenger.renderText(offer.displayName()), lore));
    }

    private MenuButton exportButton(ExportOffer offer) {
        List<Component> lore = new ArrayList<>();
        lore.add(screens.messenger.render("business.ledgers.details.export-tag"));
        lore.addAll(screens.render(offer.lore()));
        lore.add(Component.empty());
        lore.add(switch (offer.target()) {
            case ExportOffer.Target.Item item -> screens.messenger.render("business.ledgers.details.export-item",
                    Placeholder.unparsed("batch", String.valueOf(offer.batch())), Placeholder.unparsed("item", item.materialId()));
            case ExportOffer.Target.GoodType type -> screens.messenger.render("business.ledgers.details.export-good-type",
                    Placeholder.unparsed("batch", String.valueOf(offer.batch())), Placeholder.unparsed("type", type.goodType()));
        });
        return MenuButton.display(theme.item(LedgerScreens.material(offer.icon()),
                screens.messenger.renderText(offer.displayName()), lore));
    }

    @Override
    protected @Nullable MenuButton header() {
        List<Component> lore = new ArrayList<>(screens.render(ledger.description()));
        lore.add(Component.empty());
        lore.add(screens.messenger.render("business.ledgers.details.counts",
                Placeholder.unparsed("imports", String.valueOf(ledger.imports().size())),
                Placeholder.unparsed("exports", String.valueOf(ledger.exports().size()))));
        return MenuButton.display(theme.item(LedgerScreens.material(ledger.icon()),
                screens.messenger.renderText(ledger.displayName()), lore));
    }

    @Override
    protected void drawSide() {
        set(BorderedLayout.side().get(0), selectOrRemove());
    }

    private MenuButton selectOrRemove() {
        Business business = context.business();
        if (!context.isOwner()) {
            return MenuButton.display(theme.item(Material.GRAY_DYE, "business.ledgers.details.owner-only.name",
                    "business.ledgers.details.owner-only.lore"));
        }
        if (screens.selections.isSelected(business, ledger.id())) {
            return MenuButton.of(theme.item(Material.BARRIER, "business.ledgers.details.remove.name",
                    "business.ledgers.details.remove.lore", screens.slots(business)), click -> confirmRemove(click.player()));
        }
        if (screens.selections.freeSlots(business) == 0) {
            return MenuButton.display(theme.item(Material.GRAY_DYE, "business.ledgers.details.full.name",
                    "business.ledgers.details.full.lore", screens.slots(business)));
        }
        return MenuButton.of(theme.item(Material.LIME_DYE, "business.ledgers.details.select.name",
                "business.ledgers.details.select.lore", screens.slots(business)), click -> confirmSelect(click.player()));
    }

    private void confirmSelect(Player player) {
        Business business = context.business();
        new ConfirmMenu(theme, theme.title("business.ledgers.confirm.select-title", LedgerScreens.name(ledger)),
                theme.item(LedgerScreens.material(ledger.icon()), "business.ledgers.confirm.select.name",
                        "business.ledgers.confirm.select.lore", LedgerScreens.name(ledger), screens.slots(business)),
                click -> owner(click.player()).ifPresent(owner -> {
                    try {
                        screens.selections.select(business, owner, ledger.id());
                        screens.messenger.send(click.player(), "business.ledgers.selected", LedgerScreens.name(ledger),
                                Placeholder.unparsed("business", business.displayName()));
                    } catch (BusinessException refusal) {
                        screens.messenger.send(click.player(), refusal);
                    }
                    screens.openDetails(click.player(), business, ledger);
                }),
                click -> open(click.player())).open(player);
    }

    private void confirmRemove(Player player) {
        Business business = context.business();
        new ConfirmMenu(theme, theme.title("business.ledgers.confirm.remove-title", LedgerScreens.name(ledger)),
                theme.item(Material.BARRIER, "business.ledgers.confirm.remove.name",
                        "business.ledgers.confirm.remove.lore", LedgerScreens.name(ledger), screens.slots(business)),
                click -> owner(click.player()).ifPresent(owner -> {
                    try {
                        BigDecimal fine = screens.selections.remove(business, owner, ledger.id());
                        screens.messenger.send(click.player(), "business.ledgers.removed", LedgerScreens.name(ledger),
                                Placeholder.unparsed("business", business.displayName()),
                                Placeholder.unparsed("fine", screens.wallet.format(fine)));
                    } catch (BusinessException refusal) {
                        screens.messenger.send(click.player(), refusal);
                    }
                    screens.openDetails(click.player(), business, ledger);
                }),
                click -> open(click.player())).open(player);
    }

    /** @return the player's character, checked again because the owner may have changed while the menu was open */
    private Optional<CharacterRef> owner(Player player) {
        BusinessMenuContext now = screens.menus.context(player, context.business());
        if (!now.isOwner()) {
            screens.messenger.send(player, "business.not-owner",
                    Placeholder.unparsed("business", context.business().displayName()));
            return Optional.empty();
        }
        return now.character();
    }

    @Override
    protected @Nullable MenuButton back() {
        return theme.back(click -> screens.openList(click.player(), context.business()));
    }
}
