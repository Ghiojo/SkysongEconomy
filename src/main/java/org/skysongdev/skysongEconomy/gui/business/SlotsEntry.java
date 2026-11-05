package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.items.SkysongItemFactory;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.LedgerRegistry;
import org.skysongdev.skysongEconomy.trade.delivery.DeliveryService;

import java.time.Clock;
import java.util.Optional;

/**
 * The main menu's Slots button, shown when the business has Economy and Logistics: how many
 * deliveries are on the way and ready. Players who can't collect any see why.
 */
public final class SlotsEntry implements MainMenuEntry {
    private final SlotsMenu.Screens screens;

    public SlotsEntry(BusinessMenus menus, DeliveryService deliveries, EconomyLogisticsSettings settings,
                      LedgerRegistry ledgers, SkysongItemFactory items, Messenger messenger, Clock clock) {
        this.screens = new SlotsMenu.Screens(menus, deliveries, settings, ledgers, items, messenger, clock);
    }

    @Override
    public int slot() {
        return MainMenuSlots.SLOTS;
    }

    @Override
    public Optional<MenuButton> button(BusinessMenuContext context) {
        Business business = context.business();
        if (!screens.settings().enabled(business)) {
            return Optional.empty();
        }
        boolean canCollectAny = context.character().filter(character -> screens.deliveries().canOrder(business, character)
                || screens.deliveries().deliveries(business).stream().anyMatch(delivery -> delivery.orderedBy().equals(character)))
                .isPresent();
        if (!canCollectAny) {
            return Optional.of(MenuButton.display(screens.menus().theme().item(Material.GRAY_DYE,
                    "business.slots.button.name", "business.slots.button.lore-not-allowed")));
        }
        long ready = screens.deliveries().deliveries(business).stream()
                .filter(delivery -> delivery.isReady(screens.clock().instant())).count();
        int used = screens.deliveries().deliveries(business).size();
        return Optional.of(MenuButton.of(screens.menus().theme().item(ready > 0 ? Material.CHEST : Material.CLOCK,
                        "business.slots.button.name", "business.slots.button.lore",
                        Placeholder.unparsed("used", String.valueOf(used)),
                        Placeholder.unparsed("max", String.valueOf(screens.settings().limits(business).imports())),
                        Placeholder.unparsed("ready", String.valueOf(ready))),
                click -> new SlotsMenu(screens, screens.menus().context(click.player(), business)).open(click.player())));
    }
}
