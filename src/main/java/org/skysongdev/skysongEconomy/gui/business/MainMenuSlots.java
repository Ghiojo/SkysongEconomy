package org.skysongdev.skysongEconomy.gui.business;

import org.skysongdev.skysongEconomy.gui.layout.BorderedLayout;

/**
 * Where each main menu button goes, so features don't overlap. Row 2 holds what every business
 * has, row 3 what Economy and Logistics adds.
 */
public final class MainMenuSlots {
    public static final int WORK_SHIFT = BorderedLayout.slot(2, 2);
    public static final int RETRIEVE_PAY = BorderedLayout.slot(2, 3);
    public static final int TRAIN = BorderedLayout.slot(2, 4);
    public static final int VAULT = BorderedLayout.slot(2, 5);
    public static final int SKILL_PICKS = BorderedLayout.slot(2, 6);

    public static final int IMPORTS = BorderedLayout.slot(3, 2);
    public static final int EXPORTS = BorderedLayout.slot(3, 3);
    public static final int SLOTS = BorderedLayout.slot(3, 5);
    public static final int LEDGERS = BorderedLayout.slot(3, 6);

    private MainMenuSlots() {
    }
}
