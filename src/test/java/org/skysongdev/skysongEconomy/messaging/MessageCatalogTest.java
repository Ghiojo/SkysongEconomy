package org.skysongdev.skysongEconomy.messaging;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageCatalogTest {
    private static ConfigSection yaml(String text) {
        return YamlFiles.parse("messages.yml", new StringReader(text));
    }

    @Test
    void serverFileOverridesDefaultsKeyByKey() {
        List<String> warnings = new ArrayList<>();
        MessageCatalog catalog = MessageCatalog.of(
                yaml("prefix: '[S] '\nbusiness:\n  hired: Hired!\n  fired: Fired!\n"),
                yaml("business:\n  hired: Welcome aboard!\n"), warnings::add);

        assertEquals("Welcome aboard!", catalog.text("business.hired"));
        assertEquals("Fired!", catalog.text("business.fired"), "keys missing from the server file keep the default");
        assertEquals("[S] ", catalog.text(MessageCatalog.PREFIX));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void unknownServerKeysAreReportedAndMissingKeysShowThemselves() {
        List<String> warnings = new ArrayList<>();
        MessageCatalog catalog = MessageCatalog.of(yaml("a: A\n"), yaml("b: B\n"), warnings::add);

        assertEquals(1, warnings.size());
        assertTrue(catalog.text("b").contains("Missing message: b"));
    }

    @Test
    void listsAreLinesOrJoinedText() {
        MessageCatalog catalog = MessageCatalog.of(yaml("lore: [one, two]\nsingle: just\n"), yaml(""), w -> { });

        assertEquals(List.of("one", "two"), catalog.lines("lore"));
        assertEquals("one\ntwo", catalog.text("lore"));
        assertEquals(List.of("just"), catalog.lines("single"));
    }

    /** Every key the code sends must exist in the bundled messages.yml. */
    @Test
    void everyMessageKeyUsedInTheCodeExists() throws IOException {
        MessageCatalog catalog;
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/messages.yml")), StandardCharsets.UTF_8)) {
            catalog = MessageCatalog.of(YamlFiles.parse("messages.yml", reader), yaml(""), w -> { });
        }
        Pattern key = Pattern.compile("\"((?:command|business|menu|prompt|tokens)\\.[a-z0-9.-]+)\"");
        List<String> missing = new ArrayList<>();
        try (Stream<Path> sources = Files.walk(Path.of("src/main/java"))) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = key.matcher(Files.readString(source));
                while (matcher.find()) {
                    String found = matcher.group(1);
                    if (!found.endsWith(".") && !catalog.has(found)) {
                        missing.add(found + " in " + source.getFileName());
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Missing from messages.yml: " + missing);
    }
}
