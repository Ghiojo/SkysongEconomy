package org.skysongdev.skysongEconomy.character;

/** What happened when a business tried to train a character's proficiency focus. */
public enum ProficiencyTraining {
    /** The points were added, and the focus may have gone up a tier. */
    TRAINED,
    /** The character has no focus to train. */
    NO_FOCUS,
    /** The focus belongs to a skill other than the one trained. */
    OTHER_SKILL,
    /** The focus is already at the highest tier this business can teach. */
    AT_MAX_TIER,
    /** The character, skill or tier doesn't exist. */
    UNAVAILABLE
}
