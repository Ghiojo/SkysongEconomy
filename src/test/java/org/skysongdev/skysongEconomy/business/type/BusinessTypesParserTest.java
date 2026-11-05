package org.skysongdev.skysongEconomy.business.type;

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

class BusinessTypesParserTest {
    private static final Set<String> KNOWN = Set.of(
            "animalhandling", "farming", "forestry", "mining", "mistgathering", "woodprocessing",
            "alchemy", "artificing", "cooking", "craftsman", "metalworking", "tailoring", "witchcraft",
            "economical", "entertainment", "martial", "medical", "tande");

    private final List<String> warnings = new ArrayList<>();
    private final BusinessTypesParser parser = new BusinessTypesParser(KNOWN::contains, warnings::add);

    private BusinessTypeRegistry parse(String yaml) {
        return parser.parse(YamlFiles.parse("types.yml", new StringReader(yaml)));
    }

    @Test
    void bundledDefaultsGiveThreeTypesWithEverySkill() throws IOException {
        BusinessTypeRegistry registry;
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/businesses/types.yml")), StandardCharsets.UTF_8)) {
            registry = parser.parse(YamlFiles.parse(BusinessTypesParser.FILE, reader));
        }

        assertEquals(List.of("gathering", "crafting", "service"),
                registry.all().stream().map(BusinessType::id).toList());
        assertTrue(warnings.isEmpty(), warnings::toString);
        for (String skill : KNOWN) {
            assertTrue(registry.typeOfSkill(skill).isPresent(), skill + " has no type");
        }
        assertTrue(registry.find("service").orElseThrow().trainsProficiency());
        assertFalse(registry.find("crafting").orElseThrow().trainsProficiency());
    }

    @Test
    void readsATypeAndLowerCasesSkills() {
        BusinessTypeRegistry registry = parse("""
                types:
                  shop:
                    name: "<gold>Shop"
                    skills: [Economical, martial]
                    trains-proficiency: true
                """);

        BusinessType shop = registry.find("shop").orElseThrow();
        assertEquals("<gold>Shop", shop.displayName());
        assertEquals(Set.of("economical", "martial"), shop.skillIds());
        assertTrue(shop.offers("martial"));
        assertEquals(shop, registry.typeOfSkill("economical").orElseThrow());
    }

    @Test
    void unknownSkillsAreSkippedWithAWarning() {
        BusinessTypeRegistry registry = parse("""
                types:
                  shop: {name: Shop, skills: [martial, juggling]}
                """);

        assertEquals(Set.of("martial"), registry.find("shop").orElseThrow().skillIds());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("juggling"), warnings::toString);
    }

    @Test
    void aSkillStaysWithTheFirstTypeThatListsIt() {
        BusinessTypeRegistry registry = parse("""
                types:
                  first: {name: First, skills: [martial]}
                  second: {name: Second, skills: [martial, medical]}
                """);

        assertEquals("first", registry.typeOfSkill("martial").orElseThrow().id());
        assertEquals(Set.of("medical"), registry.find("second").orElseThrow().skillIds());
        assertEquals(1, warnings.size());
    }

    @Test
    void typesWithoutUsableSkillsAreKeptButReported() {
        BusinessTypeRegistry registry = parse("types:\n  empty: {name: Empty}\n");

        assertTrue(registry.find("empty").orElseThrow().skillIds().isEmpty());
        assertEquals(1, warnings.size());
    }

    @Test
    void anEmptyFileIsReported() {
        assertTrue(parse("").all().isEmpty());
        assertEquals(1, warnings.size());
    }

    @Test
    void badIdsAndMissingNamesFail() {
        assertThrows(ConfigException.class, () -> parse("types:\n  Bad Id: {name: X}\n"));
        assertThrows(ConfigException.class, () -> parse("types:\n  shop: {skills: [martial]}\n"));
    }
}
