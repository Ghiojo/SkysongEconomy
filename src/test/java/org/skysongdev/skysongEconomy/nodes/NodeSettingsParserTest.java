package org.skysongdev.skysongEconomy.nodes;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeSettingsParserTest {
    private final List<String> warnings = new ArrayList<>();
    private static final Set<String> LEVELS = Set.of("unskilled", "novice", "competent", "journeyman", "expert", "master");
    private final NodeSettingsParser parser = new NodeSettingsParser(Set.of("gathering", "crafting")::contains,
            LEVELS::contains, warnings::add);

    private NodeSettings parse(String yaml) {
        return parser.parse(YamlFiles.parse(NodeSettingsParser.FILE, new StringReader(yaml)));
    }

    @Test
    void bundledDefaultsLetGatheringBusinessesOwnNodes() throws IOException {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/nodes/settings.yml")), StandardCharsets.UTF_8)) {
            NodeSettings settings = parser.parse(YamlFiles.parse(NodeSettingsParser.FILE, reader));
            assertEquals(Set.of("gathering"), settings.ownerBusinessTypes());
            assertTrue(settings.nameplate().enabled());
            assertEquals(16, settings.nameplate().viewDistance());
            assertEquals("unskilled", settings.levelFor(1));
            assertEquals("expert", settings.levelFor(5));
            assertEquals(1, settings.primerRemovalDowntime());
            assertEquals(NodeMenuIcons.DEFAULTS, settings.menuIcons());
        }
        assertEquals(List.of(), warnings);
    }

    @Test
    void typesAreLowerCasedAndUnknownOnesAreLeftOutWithAWarning() {
        NodeSettings settings = parse("owner-business-types: [Gathering, fishing]");

        assertEquals(Set.of("gathering"), settings.ownerBusinessTypes());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("fishing"), warnings.get(0));
    }

    @Test
    void anEmptyFileMeansNoBusinessOwnsNodesAndDefaultNameplates() {
        NodeSettings settings = parse("");

        assertEquals(Set.of(), settings.ownerBusinessTypes());
        assertEquals("novice", settings.levelFor(2));
        assertTrue(settings.nameplate().enabled());
    }

    @Test
    void tierLevelsCanBeChangedAndTheOthersKeepTheirDefaults() {
        NodeSettings settings = parse("tier-skill-levels:\n  5: Master\n  2: unskilled");

        assertEquals("unskilled", settings.levelFor(1));
        assertEquals("unskilled", settings.levelFor(2));
        assertEquals("competent", settings.levelFor(3));
        assertEquals("master", settings.levelFor(5));
    }

    @Test
    void unknownLevelsAndTiersAreRefused() {
        assertThrows(ConfigException.class, () -> parse("tier-skill-levels:\n  1: grandmaster"));
        assertThrows(ConfigException.class, () -> parse("tier-skill-levels:\n  6: expert"));
        assertThrows(ConfigException.class, () -> parse("tier-skill-levels:\n  first: expert"));
    }

    @Test
    void menuIconsDefaultAndCanBeChanged() {
        assertEquals(NodeMenuIcons.DEFAULTS, parse("").menuIcons());
        assertEquals(new NodeMenuIcons("BOOK", "DIAMOND_PICKAXE", "GRAY_DYE", "FARMLAND", "SHEARS"),
                parse("menu:\n  tier: DIAMOND_PICKAXE\n  empty-plot: FARMLAND").menuIcons());
    }

    @Test
    void primerRemovalCostsOneDowntimeUnlessChanged() {
        assertEquals(1, parse("").primerRemovalDowntime());
        assertEquals(0, parse("farm:\n  primer-removal-downtime: 0").primerRemovalDowntime());
        assertThrows(ConfigException.class, () -> parse("farm:\n  primer-removal-downtime: -1"));
    }

    @Test
    void nameplatesCanBeTurnedOff() {
        assertFalse(parse("nameplate:\n  enabled: false").nameplate().enabled());
    }

    @Test
    void badNameplateValuesAreRefused() {
        assertThrows(ConfigException.class, () -> parse("nameplate:\n  view-distance: 0"));
    }
}
