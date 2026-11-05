package org.skysongdev.skysongEconomy.business.training;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrainingSettingsParserTest {
    private final TrainingSettingsParser parser = new TrainingSettingsParser();

    private TrainingSettings parse(String yaml) {
        return parser.parse(YamlFiles.parse("training.yml", new StringReader(yaml)));
    }

    @Test
    void bundledFileRewardsEveryPickCount() throws IOException {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/businesses/training.yml")), StandardCharsets.UTF_8)) {
            TrainingSettings settings = parser.parse(YamlFiles.parse(TrainingSettingsParser.FILE, reader));

            assertEquals(Duration.ofMinutes(30), settings.duration());
            assertEquals(1, settings.downtimeCost());
            for (int picks = 1; picks <= 5; picks++) {
                assertEquals(picks, settings.reward(picks).proficiency());
            }
            assertEquals(100, settings.reward(1).xp());
        }
    }

    @Test
    void missingRowsGiveNothing() {
        TrainingSettings settings = parse("rewards: {1: {xp: 10}}");

        assertEquals(new TrainingReward(10, 0), settings.reward(1));
        assertEquals(TrainingReward.NONE, settings.reward(2));
    }

    @Test
    void refusesBadValues() {
        assertThrows(ConfigException.class, () -> parse("duration-minutes: 0"));
        assertThrows(ConfigException.class, () -> parse("downtime-cost: -1"));
        assertThrows(ConfigException.class, () -> parse("rewards: {0: {xp: 1}}"));
        assertThrows(ConfigException.class, () -> parse("rewards: {6: {xp: 1}}"));
        assertThrows(ConfigException.class, () -> parse("rewards: {1: {xp: -1}}"));
        assertThrows(ConfigException.class, () -> parse("rewards: {1: {proficiency: -1}}"));
    }
}
