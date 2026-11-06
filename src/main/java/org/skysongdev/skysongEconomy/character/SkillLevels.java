package org.skysongdev.skysongEconomy.character;

import java.util.List;
import java.util.Optional;

/** The skill levels characters reach with XP, such as unskilled or expert. */
public interface SkillLevels {
    /** @return every level's id, lowest first */
    List<String> levelIds();

    /** @return the character's level in that skill; empty when the skill or character is unknown */
    Optional<String> levelOf(CharacterRef character, String skillId);

    /** @return whether a level with that id exists */
    default boolean levelExists(String levelId) {
        return levelIds().contains(levelId);
    }

    /** @return the level's name as players see it, in MiniMessage; the id when the level is unknown */
    default String levelName(String levelId) {
        return levelId;
    }

    /**
     * @return whether the character is at {@code requiredLevel} or above in the skill; never when
     *         either level is unknown
     */
    default boolean reaches(CharacterRef character, String skillId, String requiredLevel) {
        List<String> order = levelIds();
        int required = order.indexOf(requiredLevel);
        int reached = levelOf(character, skillId).map(order::indexOf).orElse(-1);
        return required >= 0 && reached >= required;
    }
}
