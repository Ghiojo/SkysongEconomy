package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.LedgerSelections;
import org.skysongdev.skysongEconomy.trade.TradePricing;
import org.skysongdev.skysongEconomy.trade.delivery.DeliveryService;

import java.util.Optional;

/**
 * The main menu's Imports button, shown when the business has Economy and Logistics. Players who
 * may order see the window; others see why they can't.
 */
public final class ImportsEntry implements MainMenuEntry {
    private final ImportScreens screens;

    public ImportsEntry(BusinessMenus menus, LedgerSelections selections, DeliveryService deliveries, TradePricing pricing,
                        EconomyLogisticsSettings settings, Wallet wallet, Messenger messenger) {
        this.screens = new ImportScreens(menus, selections, deliveries, pricing, settings, wallet, messenger);
    }

    @Override
    public int slot() {
        return MainMenuSlots.IMPORTS;
    }

    @Override
    public Optional<MenuButton> button(BusinessMenuContext context) {
        Business business = context.business();
        if (!screens.settings.enabled(business)) {
            return Optional.empty();
        }
        boolean allowed = context.character().filter(character -> screens.deliveries.canOrder(business, character)).isPresent();
        if (!allowed) {
            return Optional.of(MenuButton.display(screens.menus.theme().item(Material.GRAY_DYE,
                    "business.imports.button.name", "business.imports.button.lore-not-allowed")));
        }
        return Optional.of(MenuButton.of(screens.menus.theme().item(Material.CHEST_MINECART, "business.imports.button.name",
                        "business.imports.button.lore",
                        Placeholder.unparsed("free", String.valueOf(screens.deliveries.freeSlots(business))),
                        Placeholder.unparsed("max", String.valueOf(screens.settings.limits(business).imports()))),
                click -> screens.open(click.player(), business)));
    }
}
