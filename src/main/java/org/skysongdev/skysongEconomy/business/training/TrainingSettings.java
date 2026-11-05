package org.skysongdev.skysongEconomy.business.training;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Settings from {@code businesses/training.yml}.
 *
 * @param duration     how long a session takes, counting only time online and not AFK
 * @param downtimeCost downtime spent to start one, from the player's preferred pool
 * @param rewards      what each picked skill gives, by how many times it was picked
 */
public record TrainingSettings(Duration duration, int downtimeCost, Map<Integer, TrainingReward> rewards) {
    public TrainingSettings {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive: " + duration);
        }
        if (downtimeCost < 0) {
            throw new IllegalArgumentException("downtimeCost must not be negative: " + downtimeCost);
        }
        rewards = Map.copyOf(rewards);
    }

    /** @return what a skill picked {@code picks} times gives; nothing when the table has no row */
    public TrainingReward reward(int picks) {
        return rewards.getOrDefault(picks, TrainingReward.NONE);
    }
}
