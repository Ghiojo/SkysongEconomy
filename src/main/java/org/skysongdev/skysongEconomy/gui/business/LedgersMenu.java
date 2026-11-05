package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.PagedMenu;
import org.skysongdev.skysongEconomy.trade.Ledger;

import java.util.ArrayList;
import java.util.List;

/** Every ledger on the server, marked when the business has selected it. Clicking one opens its details. */
final class LedgersMenu extends PagedMenu<Ledger> {
    private final LedgerScreens screens;
    private final BusinessMenuContext context;

    LedgersMenu(LedgerScreens screens, BusinessMenuContext context) {
        super(screens.menus.theme(), screens.menus.theme().title("business.ledgers.menu.title",
                Placeholder.unparsed("business", context.business().displayName())));
        this.screens = screens;
        this.context = context;
    }

    @Override
    protected List<Ledger> items() {
        return List.copyOf(screens.ledgers.all());
    }

    @Override
    protected MenuButton button(Ledger ledger) {
        Business business = context.business();
        List<Component> lore = new ArrayList<>(screens.render(ledger.description()));
        lore.add(Component.empty());
        lore.add(screens.messenger.render(screens.selections.isSelected(business, ledger.id())
                ? "business.ledgers.menu.selected" : "business.ledgers.menu.not-selected"));
        lore.add(screens.messenger.render("business.ledgers.menu.click"));
        return MenuButton.of(theme.item(LedgerScreens.material(ledger.icon()),
                        screens.messenger.renderText(ledger.displayName()), lore),
                click -> screens.openDetails(click.player(), business, ledger));
    }

    @Override
    protected @Nullable MenuButton header() {
        return MenuButton.display(theme.item(Material.WRITABLE_BOOK, "business.ledgers.menu.header.name",
                "business.ledgers.menu.header.lore", screens.slots(context.business())));
    }

    @Override
    protected @Nullable MenuButton back() {
        return theme.back(click -> screens.menus.openMain(click.player(), context.business()));
    }
}
