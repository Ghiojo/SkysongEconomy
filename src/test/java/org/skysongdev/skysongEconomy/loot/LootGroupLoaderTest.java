package org.skysongdev.skysongEconomy.loot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootGroupLoaderTest {
    @TempDir
    Path folder;

    private final List<String> warnings = new ArrayList<>();
    private final LootGroupLoader loader = new LootGroupLoader(new LootParser(Set.of("coal", "ruby")::contains), warnings::add);

    @Test
    void eachFileIsOneGroupNamedAfterIt() throws IOException {
        Files.writeString(folder.resolve("novice-gems.yml"), "entries: [{item: ruby, weight: 3}]\n");
        Files.writeString(folder.resolve("ores.yml"), "entries: [{item: coal}, {group: novice-gems}]\n");
        Files.writeString(folder.resolve("notes.txt"), "not a group");
        LootGroupLibrary library = loader.load(folder);
        assertEquals(List.of(), warnings);
        assertEquals(Set.of("novice-gems", "ores"), library.ids());
        assertEquals(2, library.find("ores").orElseThrow().totalWeight());
    }

    @Test
    void skipsBadFileNamesAndBrokenYaml() throws IOException {
        Files.writeString(folder.resolve("Bad Name.yml"), "entries: [{item: coal}]\n");
        Files.writeString(folder.resolve("broken.yml"), "entries: [{item: coal\n");
        Files.writeString(folder.resolve("fine.yml"), "entries: [{item: coal}]\n");
        assertEquals(Set.of("fine"), loader.load(folder).ids());
        assertEquals(2, warnings.size());
        assertTrue(warnings.get(0).contains("Bad Name.yml"), warnings.get(0));
    }

    @Test
    void aMissingFolderMeansNoGroups() {
        assertEquals(Set.of(), loader.load(folder.resolve("missing")).ids());
        assertEquals(List.of(), warnings);
    }
}
