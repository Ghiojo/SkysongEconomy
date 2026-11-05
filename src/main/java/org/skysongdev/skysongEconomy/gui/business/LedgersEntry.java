package org.skysongdev.skysongEconomy.gui.business;

import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.LedgerRegistry;
import org.skysongdev.skysongEconomy.trade.LedgerSelections;

import java.util.Optional;

/**
 * The main menu's Ledgers button, shown when the business has Economy and Logistics. Everyone
 * can browse the ledgers; only the owner selects and removes them.
 */
public final class LedgersEntry implements MainMenuEntry {
    private final LedgerScreens screens;

    public LedgersEntry(BusinessMenus menus, LedgerRegistry ledgers, LedgerSelections selections,
                        EconomyLogisticsSettings settings, Wallet wallet, Messenger messenger) {
        this.screens = new LedgerScreens(menus, ledgers, selections, settings, wallet, messenger);
    }

    @Override
    public int slot() {
        return MainMenuSlots.LEDGERS;
    }

    @Override
    public Optional<MenuButton> button(BusinessMenuContext context) {
        Business business = context.business();
        if (!screens.settings.enabled(business)) {
            return Optional.empty();
        }
        return Optional.of(MenuButton.of(
                screens.menus.theme().item(Material.WRITABLE_BOOK, "business.ledgers.button.name",
                        "business.ledgers.button.lore", screens.slots(business)),
                click -> screens.openList(click.player(), business)));
    }
}
