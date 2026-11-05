package org.skysongdev.skysongEconomy.integration.cmi;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asks CMI whether a player is AFK: {@code CMI.getInstance().getPlayerManager().getUser(player).isAfk()}.
 * <p>
 * CMI's API isn't published to a Maven repository, so it is called by reflection instead of
 * being a build dependency. If the call stops working (for example after a CMI update renames
 * it), the error is logged once and nobody counts as AFK.
 */
public final class CmiAfkCheck implements AfkCheck {
    private final Logger logger;
    private final Object playerManager;
    private final Method getUser;
    private boolean broken;

    private CmiAfkCheck(Logger logger, Object playerManager, Method getUser) {
        this.logger = logger;
        this.playerManager = playerManager;
        this.getUser = getUser;
    }

    /** @return the CMI check, or {@link AfkCheck#NONE} when CMI can't be reached */
    public static AfkCheck create(ClassLoader cmiClasses, Logger logger) {
        try {
            Class<?> cmi = Class.forName("com.Zrips.CMI.CMI", true, cmiClasses);
            Object instance = cmi.getMethod("getInstance").invoke(null);
            Object playerManager = cmi.getMethod("getPlayerManager").invoke(instance);
            Method getUser = playerManager.getClass().getMethod("getUser", Player.class);
            logger.info("Shift and training timers pause while CMI says a player is AFK");
            return new CmiAfkCheck(logger, playerManager, getUser);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            logger.log(Level.WARNING, "CMI is installed but its AFK status can't be read, so AFK time counts", e);
            return NONE;
        }
    }

    @Override
    public boolean isAfk(Player player) {
        if (broken) {
            return false;
        }
        try {
            Object user = getUser.invoke(playerManager, player);
            return user != null && (Boolean) user.getClass().getMethod("isAfk").invoke(user);
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            logger.log(Level.WARNING, "Reading CMI's AFK status failed, so AFK time counts from now on", e);
            return false;
        }
    }
}
