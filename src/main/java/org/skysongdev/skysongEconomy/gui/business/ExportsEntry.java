package org.skysongdev.skysongEconomy.gui.business;

import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.ExportService;
import org.skysongdev.skysongEconomy.trade.LedgerSelections;

import java.util.Optional;

/**
 * The main menu's Exports button, shown when the business has Economy and Logistics. Players who
 * may sell see the window; others see why they can't.
 */
public final class ExportsEntry implements MainMenuEntry {
    private final ExportsMenu.Screens screens;

    public ExportsEntry(BusinessMenus menus, LedgerSelections selections, ExportService exports,
                        EconomyLogisticsSettings settings, MaterialIdResolver materialIds, Wallet wallet, Messenger messenger) {
        this.screens = new ExportsMenu.Screens(menus, selections, exports, settings, materialIds, wallet, messenger);
    }

    @Override
    public int slot() {
        return MainMenuSlots.EXPORTS;
    }

    @Override
    public Optional<MenuButton> button(BusinessMenuContext context) {
        Business business = context.business();
        if (!screens.settings().enabled(business)) {
            return Optional.empty();
        }
        boolean allowed = context.character().filter(character -> screens.exports().canSell(business, character)).isPresent();
        if (!allowed) {
            return Optional.of(MenuButton.display(screens.menus().theme().item(Material.GRAY_DYE,
                    "business.exports.button.name", "business.exports.button.lore-not-allowed")));
        }
        return Optional.of(MenuButton.of(screens.menus().theme().item(Material.EMERALD, "business.exports.button.name",
                        "business.exports.button.lore"),
                click -> new ExportsMenu(screens, screens.menus().context(click.player(), business)).open(click.player())));
    }
}
