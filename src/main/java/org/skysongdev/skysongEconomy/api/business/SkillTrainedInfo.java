package org.skysongdev.skysongEconomy.api.business;

import java.util.Objects;

/**
 * What a finished training session gave for one skill.
 *
 * @param skillId            the skill
 * @param picks              how many times the business picked it
 * @param xp                 the skill XP given; 0 when the skill or character no longer exists
 * @param proficiencyTrained whether proficiency points were given (only at Service businesses)
 */
public record SkillTrainedInfo(String skillId, int picks, int xp, boolean proficiencyTrained) {
    public SkillTrainedInfo {
        Objects.requireNonNull(skillId, "skillId");
    }
}
