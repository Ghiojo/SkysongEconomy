package org.skysongdev.skysongEconomy.business.activity;

import org.skysongdev.skysongEconomy.business.BusinessException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Makes sure a player account runs one activity at a time: one shift or one training session,
 * whichever character they play. Shifts and training share one lock. Not thread-safe: use it on
 * the main thread.
 */
public final class ActivityLock {
    private final Map<UUID, Activity> current = new HashMap<>();

    /** @return what the player is busy with, empty when they are free */
    public Optional<Activity> current(UUID player) {
        return Optional.ofNullable(current.get(player));
    }

    /**
     * @throws BusinessException {@code business.activity.busy-shift} or
     *                           {@code business.activity.busy-training} when the player is busy
     */
    public void requireFree(UUID player) {
        Activity busy = current.get(player);
        if (busy != null) {
            String key = busy.kind() == Activity.Kind.SHIFT ? "business.activity.busy-shift" : "business.activity.busy-training";
            throw new BusinessException(key, Map.of("business", busy.businessId()));
        }
    }

    /**
     * Marks the player busy.
     *
     * @throws BusinessException when they already are; see {@link #requireFree}
     */
    public void claim(UUID player, Activity activity) {
        requireFree(player);
        current.put(player, activity);
    }

    /** Frees the player, if {@code activity} is what they are busy with. */
    public void release(UUID player, Activity activity) {
        current.remove(player, activity);
    }
}
