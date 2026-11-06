package org.skysongdev.skysongEconomy.nodes;

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

class NodeFilesTest {
    @TempDir
    Path dataFolder;

    private final List<String> warnings = new ArrayList<>();

    private NodeFiles files() {
        return new NodeFiles(dataFolder, Set.of("coal", "copper")::contains, Set.of("mining")::contains, warnings::add);
    }

    private void write(String path, String text) throws IOException {
        Path file = dataFolder.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    @Test
    void missingFoldersLoadNothing() {
        NodeFiles.Loaded loaded = files().load();

        assertEquals(Set.of(), loaded.groups().ids());
        assertEquals(0, loaded.random().size());
        assertEquals(0, loaded.warnings());
    }

    @Test
    void tablesRollOnTheGroupsAndBrokenFilesAreCountedAndSkipped() throws IOException {
        write("nodes/groups/ores.yml", "entries:\n  - {item: coal}\n  - {item: copper}");
        write("nodes/random/mine.yml", "skill: mining\ntiers:\n  1: {cost: 1, rolls: [{times: 2, group: ores}]}");
        write("nodes/random/broken.yml", "skill: mining\ntiers:\n  1: {cost: 1, rolls: [{group: gems}]}");

        NodeFiles.Loaded loaded = files().load();

        assertEquals(Set.of("ores"), loaded.groups().ids());
        assertTrue(loaded.random().find("mine").isPresent());
        assertTrue(loaded.random().find("broken").isEmpty());
        assertEquals(1, loaded.warnings());
        assertTrue(warnings.get(0).contains("broken"), warnings.get(0));
    }

    @Test
    void loadingAgainReadsTheFilesAsTheyAreNow() throws IOException {
        NodeFiles files = files();
        assertEquals(0, files.load().random().size());

        write("nodes/random/mine.yml", "skill: mining\ntiers:\n  1: {cost: 1, guaranteed: [{item: coal}]}");

        assertEquals(1, files.load().random().size());
    }
}
