package org.skysongdev.skysongEconomy.gui.business;

import org.skysongdev.skysongEconomy.gui.MenuButton;

import java.util.Optional;

/**
 * One button of the business main menu, such as Work a Shift. Each feature adds its own entry,
 * so the main menu never changes when a feature is added.
 */
public interface MainMenuEntry {
    /** @return the slot the button goes in; see {@link MainMenuSlots} */
    int slot();

    /** @return the button for this viewer, or empty to leave the slot out for them */
    Optional<MenuButton> button(BusinessMenuContext context);
}
