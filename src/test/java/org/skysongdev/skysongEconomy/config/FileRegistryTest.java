package org.skysongdev.skysongEconomy.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileRegistryTest {
    private record Entry(String id, String value) implements FileEntry {
    }

    @TempDir
    Path folder;

    @Test
    void findsEntriesByIdInFileOrder() {
        FileRegistry<Entry> registry = new FileRegistry<>(List.of(new Entry("b", "1"), new Entry("a", "2")));

        assertEquals(Optional.of(new Entry("a", "2")), registry.find("a"));
        assertTrue(registry.find("c").isEmpty());
        assertEquals(List.of("b", "a"), List.copyOf(registry.ids()));
        assertEquals(2, registry.size());
        assertEquals(0, FileRegistry.empty().size());
    }

    @Test
    void parseEachSkipsTheFilesTheParserRefuses() throws IOException {
        Files.writeString(folder.resolve("good.yml"), "value: yes");
        Files.writeString(folder.resolve("bad.yml"), "other: 1");
        List<String> warnings = new ArrayList<>();

        FileRegistry<Entry> registry = new YamlFolder("things", "thing", warnings::add)
                .parseEach(folder, (id, root) -> new Entry(id, root.requireString("value")));

        assertEquals(Set.of("good"), registry.ids());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).startsWith("Skipped the thing 'bad'"), warnings.get(0));
    }
}
