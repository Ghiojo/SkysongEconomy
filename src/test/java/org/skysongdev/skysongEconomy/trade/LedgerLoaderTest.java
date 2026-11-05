package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerLoaderTest {
    private final List<String> warnings = new ArrayList<>();
    private final LedgerLoader loader = new LedgerLoader(
            new LedgerParser(Set.of("slow"), id -> true, warnings::add), warnings::add);

    @Test
    void loadsEveryYmlFileSortedByName(@TempDir Path folder) throws Exception {
        Files.writeString(folder.resolve("smugglers.yml"), "name: Smugglers");
        Files.writeString(folder.resolve("guild.yml"), "name: Guild");
        Files.writeString(folder.resolve("notes.txt"), "not a ledger");
        LedgerRegistry ledgers = loader.load(folder);
        assertEquals(List.of("guild", "smugglers"), ledgers.all().stream().map(Ledger::id).toList());
        assertEquals("Smugglers", ledgers.find("smugglers").orElseThrow().displayName());
        assertEquals(List.of(), warnings);
    }

    @Test
    void skipsBrokenFilesWithAWarning(@TempDir Path folder) throws Exception {
        Files.writeString(folder.resolve("broken.yml"), "name: [unclosed");
        Files.writeString(folder.resolve("nameless.yml"), "icon: CHEST");
        Files.writeString(folder.resolve("Bad Name.yml"), "name: Bad");
        Files.writeString(folder.resolve("fine.yml"), "name: Fine");
        LedgerRegistry ledgers = loader.load(folder);
        assertEquals(1, ledgers.size());
        assertEquals(3, warnings.size(), String.join("\n", warnings));
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("broken.yml")));
    }

    @Test
    void aMissingFolderHasNoLedgers(@TempDir Path folder) {
        assertEquals(0, loader.load(folder.resolve("ledgers")).size());
    }
}
