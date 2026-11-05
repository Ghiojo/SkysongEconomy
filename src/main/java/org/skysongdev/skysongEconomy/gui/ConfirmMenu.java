package org.skysongdev.skysongEconomy.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * Asks a player to confirm something that can't be undone: a summary in the middle, confirm on
 * the left and cancel on the right. Closing the menu counts as cancelling.
 */
public final class ConfirmMenu extends Menu {
    private static final int CONFIRM = 11;
    private static final int SUMMARY = 13;
    private static final int CANCEL = 15;

    private final MenuTheme theme;
    private final ItemStack summary;
    private final Consumer<MenuClick> onConfirm;
    private final Consumer<MenuClick> onCancel;

    /**
     * @param summary   the item that describes what is being confirmed
     * @param onConfirm runs when the player confirms; the menu is closed first
     * @param onCancel  runs when the player cancels, e.g. to reopen the previous menu
     */
    public ConfirmMenu(MenuTheme theme, Component title, ItemStack summary,
                       Consumer<MenuClick> onConfirm, Consumer<MenuClick> onCancel) {
        super(27, title);
        this.theme = theme;
        this.summary = summary;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected void draw() {
        MenuButton border = theme.border();
        for (int slot = 0; slot < size(); slot++) {
            set(slot, border);
        }
        set(SUMMARY, MenuButton.display(summary));
        set(CONFIRM, theme.confirm(click -> {
            click.player().closeInventory();
            onConfirm.accept(click);
        }));
        set(CANCEL, theme.cancel(onCancel));
    }
}
