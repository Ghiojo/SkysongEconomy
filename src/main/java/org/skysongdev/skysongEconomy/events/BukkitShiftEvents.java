package org.skysongdev.skysongEconomy.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.shift.Shift;
import org.skysongdev.skysongEconomy.business.shift.ShiftEvents;
import org.skysongdev.skysongEconomy.character.CharacterRef;

/** Fires {@link ShiftStartEvent} and {@link ShiftEndEvent}. */
public final class BukkitShiftEvents implements ShiftEvents {
    private final PluginManager plugins;

    public BukkitShiftEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public boolean starting(Business business, CharacterRef character, String message) {
        Player player = Bukkit.getPlayer(character.player());
        if (player == null) {
            // Shifts start from a menu, so the player is online; refuse rather than fire without one.
            return false;
        }
        ShiftStartEvent event = new ShiftStartEvent(business, player, character, message);
        plugins.callEvent(event);
        return !event.isCancelled();
    }

    @Override
    public void ended(Business business, Shift shift) {
        plugins.callEvent(new ShiftEndEvent(business, shift));
    }
}
