package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.shift.ShiftPay;
import org.skysongdev.skysongEconomy.business.shift.ShiftService;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.MenuClick;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.util.Optional;

/**
 * The main menu's Retrieve Pay button. Everyone sees it, so former workers can still collect
 * what they are owed.
 */
public final class RetrievePayEntry implements MainMenuEntry {
    private final BusinessMenus menus;
    private final ShiftService shifts;
    private final Wallet wallet;
    private final Messenger messenger;

    public RetrievePayEntry(BusinessMenus menus, ShiftService shifts, Wallet wallet, Messenger messenger) {
        this.menus = menus;
        this.shifts = shifts;
        this.wallet = wallet;
        this.messenger = messenger;
    }

    @Override
    public int slot() {
        return MainMenuSlots.RETRIEVE_PAY;
    }

    @Override
    public Optional<MenuButton> button(BusinessMenuContext context) {
        Business business = context.business();
        ShiftPay owed = context.character().map(character -> shifts.owed(business, character)).orElse(ShiftPay.NONE);
        if (owed.isNothing()) {
            return Optional.of(MenuButton.display(menus.theme().item(Material.GRAY_DYE, "business.shift.retrieve.name",
                    "business.shift.retrieve.lore-nothing")));
        }
        return Optional.of(MenuButton.of(
                menus.theme().item(Material.GOLD_INGOT, "business.shift.retrieve.name", "business.shift.retrieve.lore",
                        ShiftPayText.placeholder(messenger, wallet, owed)),
                click -> retrieve(click, business)));
    }

    private void retrieve(MenuClick click, Business business) {
        Optional<CharacterRef> character = menus.context(click.player(), business).character();
        if (character.isEmpty()) {
            messenger.send(click.player(), "command.you-have-no-character");
            return;
        }
        try {
            ShiftPay paid = shifts.retrieve(business, character.get());
            messenger.send(click.player(), "business.shift.retrieved",
                    Placeholder.component("pay", ShiftPayText.describe(messenger, wallet, paid)));
        } catch (BusinessException refusal) {
            messenger.send(click.player(), refusal);
        }
        menus.openMain(click.player(), business);
    }
}
