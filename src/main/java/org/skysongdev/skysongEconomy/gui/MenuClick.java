package org.skysongdev.skysongEconomy.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/** A click on a menu button: who clicked, and how (left, right, shift...). */
public record MenuClick(Player player, ClickType type) {
}
