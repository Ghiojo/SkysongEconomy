package org.skysongdev.skysongEconomy.character;

import java.util.List;

/** The skill levels characters reach with XP, such as unskilled or expert. */
public interface SkillLevels {
    /** @return every level's id, lowest first */
    List<String> levelIds();

    /** @return whether a level with that id exists */
    default boolean levelExists(String levelId) {
        return levelIds().contains(levelId);
    }

    /** @return the level's name as players see it, in MiniMessage; the id when the level is unknown */
    default String levelName(String levelId) {
        return levelId;
    }
}
