package org.skysongdev.skysongEconomy.character;

/** Knows which skill ids exist, so configuration can be checked against them. */
@FunctionalInterface
public interface SkillCatalog {
    /**
     * @param skillId a skill id, as written in a config file
     * @return whether a skill with that id exists
     */
    boolean exists(String skillId);

    /**
     * @return the skill's name as players see it, in MiniMessage; the id when the skill is unknown
     */
    default String displayName(String skillId) {
        return skillId;
    }
}
