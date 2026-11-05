package org.skysongdev.skysongEconomy.config;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSectionTest {
    private static ConfigSection parse(String yaml) {
        return YamlFiles.parse("test.yml", new StringReader(yaml));
    }

    @Test
    void readsTypedValuesWithFallbacks() {
        ConfigSection root = parse("""
                name: Shop
                count: 3
                price: 2.5
                open: true
                tags: [a, b]
                """);

        assertEquals("Shop", root.string("name", "x"));
        assertEquals(3, root.integer("count", 0));
        assertEquals(2.5, root.number("price", 0));
        assertEquals(3.0, root.number("count", 0), "whole numbers are numbers too");
        assertTrue(root.bool("open", false));
        assertEquals(List.of("a", "b"), root.strings("tags"));
        assertEquals("fallback", root.string("missing", "fallback"));
        assertEquals(7, root.integer("missing", 7));
        assertEquals(List.of(), root.strings("missing"));
    }

    @Test
    void keepsKeysInFileOrder() {
        ConfigSection root = parse("b: 1\na: 2\nc: 3\n");

        assertEquals(List.of("b", "a", "c"), List.copyOf(root.keys()));
    }

    @Test
    void errorsNameTheFileAndFullKey() {
        ConfigSection root = parse("types:\n  shop:\n    count: lots\n");

        ConfigException error = assertThrows(ConfigException.class,
                () -> root.section("types").section("shop").integer("count", 0));

        assertEquals("test.yml: types.shop.count must be a whole number", error.getMessage());
    }

    @Test
    void requiredTextMustBePresentAndNotBlank() {
        ConfigSection root = parse("name: '  '\n");

        assertThrows(ConfigException.class, () -> root.requireString("name"));
        assertThrows(ConfigException.class, () -> root.requireString("missing"));
    }

    @Test
    void missingSectionIsEmptyButWrongTypeFails() {
        ConfigSection root = parse("value: 3\n");

        assertTrue(root.section("missing").keys().isEmpty());
        assertThrows(ConfigException.class, () -> root.section("value"));
    }

    @Test
    void emptyFileIsAnEmptySection() {
        assertTrue(parse("").keys().isEmpty());
    }

    @Test
    void rejectsInvalidYamlAndNonSectionRoots() {
        assertThrows(ConfigException.class, () -> parse("a: [unclosed"));
        assertThrows(ConfigException.class, () -> parse("- just\n- a list\n"));
    }

    @Test
    void refusesJavaObjectTags() {
        assertThrows(ConfigException.class, () -> parse("x: !!java.io.File /tmp\n"));
    }
}
