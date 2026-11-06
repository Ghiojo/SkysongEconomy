package org.skysongdev.skysongEconomy.character;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillLevelsTest {
    private static final CharacterRef MINER = new CharacterRef(UUID.randomUUID(), "Miner");

    private final SkillLevels levels = new SkillLevels() {
        @Override
        public List<String> levelIds() {
            return List.of("unskilled", "novice", "competent");
        }

        @Override
        public Optional<String> levelOf(CharacterRef character, String skillId) {
            return Optional.ofNullable(Map.of("mining", "novice").get(skillId));
        }
    };

    @Test
    void aCharacterReachesTheirLevelAndEveryLevelBelowIt() {
        assertTrue(levels.reaches(MINER, "mining", "unskilled"));
        assertTrue(levels.reaches(MINER, "mining", "novice"));
        assertFalse(levels.reaches(MINER, "mining", "competent"));
    }

    @Test
    void unknownSkillsAndLevelsAreNeverReached() {
        assertFalse(levels.reaches(MINER, "cooking", "unskilled"));
        assertFalse(levels.reaches(MINER, "mining", "grandmaster"));
    }
}
