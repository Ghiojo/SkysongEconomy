package org.skysongdev.skysongEconomy.gui;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * One slot of a menu: the item shown and what clicking it does.
 *
 * @param onClick {@code null} for an item that only shows information
 */
public record MenuButton(ItemStack icon, @Nullable Consumer<MenuClick> onClick) {
    public MenuButton {
        Objects.requireNonNull(icon, "icon");
    }

    /** @return a button that does nothing when clicked */
    public static MenuButton display(ItemStack icon) {
        return new MenuButton(icon, null);
    }

    /** @return a button that runs {@code onClick} */
    public static MenuButton of(ItemStack icon, Consumer<MenuClick> onClick) {
        return new MenuButton(icon, onClick);
    }
}
