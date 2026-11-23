package org.skysongdev.skysongEconomy.trade.pier;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.piers.LossModifier;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PierSettingsParserTest {
    private final List<String> warnings = new ArrayList<>();

    private PierSettings parse(String yaml) {
        return new PierSettingsParser(warnings::add).parse(YamlFiles.parse(PierSettingsParser.FILE, new StringReader(yaml)));
    }

    @Test
    void readsEveryField() {
        PierSettings settings = parse("""
                piers:
                  Harbour:
                    name: "<aqua>Harbour Pier"
                    icon: oak_boat
                    description: ["<gray>Busy."]
                    data: {faction: crown, guards: {max: 6}}
                    loss-chance: 2.5
                    loss-modifiers:
                      rough_waters: "+3"
                      patrols: {amount: "+10%", tag: contraband}
                """);

        ConfiguredPier harbour = settings.piers().get("harbour");
        assertEquals("<aqua>Harbour Pier", harbour.details().displayName());
        assertEquals("OAK_BOAT", harbour.details().icon());
        assertEquals(List.of("<gray>Busy."), harbour.details().description());
        assertEquals(Map.of("faction", "crown", "guards.max", "6"), harbour.data());
        assertEquals(new BigDecimal("2.5"), harbour.lossChance());
        assertEquals(new LossModifier("rough_waters", new BigDecimal("3"), false), harbour.lossModifiers().get("rough_waters"));
        assertEquals(new LossModifier("patrols", new BigDecimal("10"), true, Optional.of("contraband")),
                harbour.lossModifiers().get("patrols"));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void defaultsToTheIdPaperAndNoChance() {
        ConfiguredPier pier = parse("piers:\n  cove: {}\n").piers().get("cove");

        assertEquals("cove", pier.details().displayName());
        assertEquals("PAPER", pier.details().icon());
        assertEquals(BigDecimal.ZERO, pier.lossChance());
        assertTrue(pier.lossModifiers().isEmpty());
    }

    @Test
    void skipsBrokenPiersAndModifiersWithAWarning() {
        PierSettings settings = parse("""
                piers:
                  "bad id": {name: Bad}
                  stormy: {loss-chance: 140}
                  cove:
                    loss-modifiers:
                      guards: "lots"
                      storm: "+5%"
                """);

        assertEquals(List.of("cove"), List.copyOf(settings.piers().keySet()));
        assertEquals(List.of("storm"), List.copyOf(settings.piers().get("cove").lossModifiers().keySet()));
        assertEquals(3, warnings.size());
    }

    @Test
    void readsTheBundledFile() throws Exception {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(PierSettingsParser.FILE)), StandardCharsets.UTF_8)) {
            PierSettings settings = new PierSettingsParser(warnings::add).parse(YamlFiles.parse(PierSettingsParser.FILE, reader));
            assertEquals(List.of("harbour", "smugglers_cove"), List.copyOf(settings.piers().keySet()));
        }
        assertTrue(warnings.isEmpty(), warnings::toString);
    }
}
