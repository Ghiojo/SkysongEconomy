package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.activity.ActivityLock;
import org.skysongdev.skysongEconomy.business.shift.Shift;
import org.skysongdev.skysongEconomy.business.shift.ShiftPay;
import org.skysongdev.skysongEconomy.business.shift.ShiftService;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.MenuClick;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.time.Duration;
import java.util.Optional;

/**
 * The main menu's Work a Shift button. Everyone sees it; for anyone who can't start a shift it
 * is greyed out and says why.
 */
public final class WorkShiftEntry implements MainMenuEntry {
    private final BusinessMenus menus;
    private final ShiftService shifts;
    private final ActivityLock activities;
    private final Wallet wallet;
    private final Messenger messenger;

    public WorkShiftEntry(BusinessMenus menus, ShiftService shifts, ActivityLock activities, Wallet wallet,
                          Messenger messenger) {
        this.menus = menus;
        this.shifts = shifts;
        this.activities = activities;
        this.wallet = wallet;
        this.messenger = messenger;
    }

    @Override
    public int slot() {
        return MainMenuSlots.WORK_SHIFT;
    }

    @Override
    public Optional<MenuButton> button(BusinessMenuContext context) {
        Business business = context.business();
        if (context.character().isEmpty()) {
            return greyed("business.shift.button.lore-no-character");
        }
        CharacterRef character = context.character().get();
        if (!business.isMember(character)) {
            return greyed("business.shift.button.lore-not-worker");
        }
        Optional<Shift> working = shifts.active(character.player());
        if (working.isPresent() && working.get().businessId().equals(business.id())) {
            return Optional.of(MenuButton.display(menus.theme().item(Material.CLOCK, "business.shift.button.name",
                    "business.shift.button.lore-working", minutes(working.get().remaining()))));
        }
        if (activities.current(character.player()).isPresent()) {
            return greyed("business.shift.button.lore-busy");
        }
        ShiftPay pay = shifts.settings().pay(business.typeId(), business.tier());
        return Optional.of(MenuButton.of(
                menus.theme().item(Material.IRON_PICKAXE, "business.shift.button.name", "business.shift.button.lore",
                        minutes(shifts.settings().duration()),
                        Placeholder.unparsed("cost", String.valueOf(shifts.settings().downtimeCost())),
                        ShiftPayText.placeholder(messenger, wallet, pay)),
                click -> start(click, business)));
    }

    private void start(MenuClick click, Business business) {
        // Check again: the menu may have stayed open while things changed.
        BusinessMenuContext now = menus.context(click.player(), business);
        Optional<CharacterRef> character = now.character();
        if (character.isEmpty()) {
            messenger.send(click.player(), "command.you-have-no-character");
            return;
        }
        try {
            Shift shift = shifts.start(business, character.get(), click.player().getName());
            messenger.send(click.player(), "business.shift.started", Placeholder.unparsed("business", business.displayName()),
                    minutes(shift.required()));
        } catch (BusinessException refusal) {
            messenger.send(click.player(), refusal);
        }
        menus.openMain(click.player(), business);
    }

    private Optional<MenuButton> greyed(String loreKey) {
        return Optional.of(MenuButton.display(menus.theme().item(Material.GRAY_DYE, "business.shift.button.name-unavailable",
                loreKey)));
    }

    /** @return a {@code <minutes>} placeholder, rounded up so "0 minutes" never shows early */
    private static TagResolver minutes(Duration duration) {
        long minutes = (duration.toSeconds() + 59) / 60;
        return Placeholder.unparsed("minutes", String.valueOf(minutes));
    }
}
