package org.skysongdev.skysongEconomy.integration.cmi;

import org.bukkit.entity.Player;

/** Whether a player is AFK, so their shift and training timers stop. */
@FunctionalInterface
public interface AfkCheck {
    /** Used when no AFK plugin is installed: nobody is AFK. */
    AfkCheck NONE = player -> false;

    boolean isAfk(Player player);
}
