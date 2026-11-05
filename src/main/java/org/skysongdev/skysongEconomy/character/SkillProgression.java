package org.skysongdev.skysongEconomy.character;

/** Gives characters skill XP and proficiency progress. */
public interface SkillProgression {
    /**
     * Adds XP to one of a character's skills.
     *
     * @param amount the XP, 0 or more
     * @return whether the XP was added; false when the character or skill doesn't exist
     */
    boolean addXp(CharacterRef character, String skillId, int amount);

    /**
     * Gives points to the character's proficiency focus, if it belongs to {@code skillId} and
     * is below {@code maxTierId}.
     *
     * @param points    the points, 1 or more
     * @param maxTierId the highest proficiency tier the trainer can teach
     * @return what the training did
     */
    ProficiencyTraining trainProficiency(CharacterRef character, String skillId, int points, String maxTierId);
}
