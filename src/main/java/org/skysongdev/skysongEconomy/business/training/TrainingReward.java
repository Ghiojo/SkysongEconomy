package org.skysongdev.skysongEconomy.business.training;

/**
 * What one training session gives for one picked skill.
 *
 * @param xp          skill XP
 * @param proficiency proficiency points, only at Service businesses
 */
public record TrainingReward(int xp, int proficiency) {
    /** Gives nothing. */
    public static final TrainingReward NONE = new TrainingReward(0, 0);

    public TrainingReward {
        if (xp < 0 || proficiency < 0) {
            throw new IllegalArgumentException("Rewards must not be negative: " + xp + " XP, " + proficiency + " points");
        }
    }
}
