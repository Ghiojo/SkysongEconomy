package org.skysongdev.skysongEconomy.listeners;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.shift.ShiftService;
import org.skysongdev.skysongEconomy.integration.cmi.AfkCheck;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.time.Duration;

/**
 * Every few seconds, adds that time to the shift of every online player who isn't AFK, and
 * tells them when it ends. Offline time never counts because offline players aren't ticked.
 */
public final class ShiftTimer implements Runnable {
    /** How often the timer runs; a shift can end up to this much late. */
    public static final Duration INTERVAL = Duration.ofSeconds(10);

    private static final long TICKS_PER_SECOND = 20;

    private final Plugin plugin;
    private final ShiftService shifts;
    private final BusinessService businesses;
    private final AfkCheck afk;
    private final Messenger messenger;

    public ShiftTimer(Plugin plugin, ShiftService shifts, BusinessService businesses, AfkCheck afk, Messenger messenger) {
        this.plugin = plugin;
        this.shifts = shifts;
        this.businesses = businesses;
        this.afk = afk;
        this.messenger = messenger;
    }

    /** Starts the timer on the main thread. */
    public BukkitTask start() {
        long ticks = INTERVAL.toSeconds() * TICKS_PER_SECOND;
        return plugin.getServer().getScheduler().runTaskTimer(plugin, this, ticks, ticks);
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (shifts.active(player.getUniqueId()).isEmpty() || afk.isAfk(player)) {
                continue;
            }
            shifts.work(player.getUniqueId(), INTERVAL).ifPresent(shift -> messenger.send(player, "business.shift.finished",
                    Placeholder.unparsed("business", businesses.find(shift.businessId())
                            .map(business -> business.displayName()).orElse(shift.businessId()))));
        }
    }
}
