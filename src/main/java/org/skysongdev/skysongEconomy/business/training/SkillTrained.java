package org.skysongdev.skysongEconomy.business.training;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.character.ProficiencyTraining;

import java.util.Optional;

/**
 * What a finished session did for one picked skill.
 *
 * @param skillId     the skill
 * @param picks       how many times the business picked it
 * @param xp          the XP given, 0 when the skill or character no longer exists
 * @param proficiency what proficiency training did; {@code null} when none was tried
 */
public record SkillTrained(String skillId, int picks, int xp, @Nullable ProficiencyTraining proficiency) {
    public Optional<ProficiencyTraining> proficiencyResult() {
        return Optional.ofNullable(proficiency);
    }
}
