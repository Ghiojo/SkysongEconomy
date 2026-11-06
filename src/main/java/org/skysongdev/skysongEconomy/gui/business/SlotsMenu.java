package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.gui.BorderedMenu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.layout.BorderedLayout;
import org.skysongdev.skysongEconomy.items.ItemHandOver;
import org.skysongdev.skysongEconomy.items.SkysongItemFactory;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.trade.Durations;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.ImportOffer;
import org.skysongdev.skysongEconomy.trade.ItemAmount;
import org.skysongdev.skysongEconomy.trade.LedgerRegistry;
import org.skysongdev.skysongEconomy.trade.delivery.Delivery;
import org.skysongdev.skysongEconomy.trade.delivery.DeliveryService;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The business's delivery slots: each delivery on the way with its arrival time, each ready one
 * to collect with a click, and the free slots. Collected items that don't fit drop at the
 * player's feet.
 */
final class SlotsMenu extends BorderedMenu {
    /** What the Slots window needs. */
    record Screens(BusinessMenus menus, DeliveryService deliveries, EconomyLogisticsSettings settings,
                   LedgerRegistry ledgers, SkysongItemFactory items, Messenger messenger, Clock clock) {
    }

    private final Screens screens;
    private final BusinessMenuContext context;

    SlotsMenu(Screens screens, BusinessMenuContext context) {
        super(screens.menus().theme(), screens.menus().theme().title("business.slots.menu.title",
                Placeholder.unparsed("business", context.business().displayName())));
        this.screens = screens;
        this.context = context;
    }

    @Override
    protected void drawContent() {
        Business business = context.business();
        List<Delivery> deliveries = screens.deliveries().deliveries(business);
        int max = screens.settings().limits(business).imports();
        List<Integer> slots = BorderedLayout.content();
        int shown = Math.min(slots.size(), Math.max(max, deliveries.size()));
        for (int i = 0; i < shown; i++) {
            set(slots.get(i), i < deliveries.size() ? deliveryButton(deliveries.get(i)) : MenuButton.display(
                    theme.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "business.slots.menu.free.name",
                            "business.slots.menu.free.lore")));
        }
        set(BorderedLayout.HEADER, MenuButton.display(theme.item(Material.CHEST, "business.slots.menu.header.name",
                "business.slots.menu.header.lore", Placeholder.unparsed("used", String.valueOf(deliveries.size())),
                Placeholder.unparsed("max", String.valueOf(max)))));
        set(BorderedLayout.BACK, theme.back(click -> screens.menus().openMain(click.player(), business)));
    }

    private MenuButton deliveryButton(Delivery delivery) {
        List<Component> lore = new ArrayList<>();
        for (ItemAmount item : delivery.items()) {
            lore.add(screens.messenger().render("business.slots.menu.item",
                    Placeholder.unparsed("amount", String.valueOf(item.amount())), Placeholder.unparsed("item", item.materialId())));
        }
        lore.add(Component.empty());
        lore.add(screens.messenger().render("business.slots.menu.ordered-by",
                Placeholder.unparsed("character", delivery.orderedBy().profile())));
        Component name = screens.messenger().renderText(importName(delivery));
        if (!delivery.isReady(screens.clock().instant())) {
            lore.add(screens.messenger().render("business.slots.menu.arrives", Placeholder.unparsed("duration",
                    Durations.format(Duration.between(screens.clock().instant(), delivery.arrivesAt())))));
            return MenuButton.display(theme.item(Material.CLOCK, name, lore));
        }
        lore.add(screens.messenger().render("business.slots.menu.ready"));
        return MenuButton.of(theme.item(Material.CHEST_MINECART, name, lore), click -> collect(click.player(), delivery));
    }

    /** @return the import's name from its ledger, or its id when the ledger or import is gone */
    private String importName(Delivery delivery) {
        return screens.ledgers().find(delivery.ledgerId())
                .flatMap(ledger -> ledger.importOffer(delivery.importId()))
                .map(ImportOffer::displayName)
                .orElse(delivery.importId());
    }

    private void collect(Player player, Delivery delivery) {
        Business business = context.business();
        Optional<CharacterRef> character = screens.menus().context(player, business).character();
        if (character.isEmpty()) {
            screens.messenger().send(player, "business.slots.not-allowed",
                    Placeholder.unparsed("business", business.displayName()));
            return;
        }
        try {
            boolean[] dropped = {false};
            screens.deliveries().collect(business, character.get(), delivery, items -> dropped[0] = give(player, items));
            screens.messenger().send(player, dropped[0] ? "business.slots.collected-dropped" : "business.slots.collected",
                    Placeholder.parsed("import", importName(delivery)));
        } catch (BusinessException refusal) {
            screens.messenger().send(player, refusal);
        } catch (IllegalArgumentException missingItem) {
            screens.messenger().send(player, "business.slots.item-missing");
        }
        redraw();
    }

    /**
     * @return whether some items didn't fit and were dropped at the player's feet
     * @throws IllegalArgumentException when an item has no file
     */
    private boolean give(Player player, List<ItemAmount> items) {
        return new ItemHandOver(screens.items()).give(player, items);
    }
}
