package org.skysongdev.skysongEconomy.business.training;

import java.util.List;

/**
 * A finished training session and what it gave.
 *
 * @param skills one entry per picked skill, in pick order
 */
public record TrainingResult(TrainingSession session, List<SkillTrained> skills) {
    public TrainingResult {
        skills = List.copyOf(skills);
    }
}
