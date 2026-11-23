package org.skysongdev.skysongEconomy.trade.pier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.piers.LossModifier;
import org.skysongdev.skysongEconomy.api.piers.Pier;
import org.skysongdev.skysongEconomy.api.piers.PierDetails;
import org.skysongdev.skysongEconomy.api.piers.PierException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PierServiceTest {
    private static final PierDetails HARBOUR = new PierDetails("<aqua>Harbour", "OAK_BOAT", List.of("Busy."));
    private static final PierDetails DOCKS = new PierDetails("<white>Guild Docks", "BARREL", List.of());

    /** Every change the service reported: "before -> after", each the pier id or "none". */
    private final List<String> changes = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final InMemoryPierRepository storage = new InMemoryPierRepository();
    private PierService piers;

    private static PierSettings file(ConfiguredPier... configured) {
        Map<String, ConfiguredPier> map = new LinkedHashMap<>();
        for (ConfiguredPier pier : configured) {
            map.put(pier.id(), pier);
        }
        return new PierSettings(map);
    }

    private static ConfiguredPier harbour(LossModifier... modifiers) {
        Map<String, LossModifier> map = new LinkedHashMap<>();
        for (LossModifier modifier : modifiers) {
            map.put(modifier.id(), modifier);
        }
        return new ConfiguredPier("harbour", HARBOUR, Map.of("faction", "crown"), new BigDecimal("2"), map);
    }

    private static LossModifier points(String id, int amount, String tag) {
        return new LossModifier(id, BigDecimal.valueOf(amount), false, Optional.ofNullable(tag));
    }

    private void start(PierSettings settings) {
        piers = new PierService(settings, storage, warnings::add);
        piers.loadAll();
        piers.addListener((before, after) -> changes.add(before.map(Pier::id).orElse("none") + " -> "
                + after.map(Pier::id).orElse("none")));
    }

    @BeforeEach
    void startWithTheHarbour() {
        start(file(harbour(points("rough_waters", 3, null))));
    }

    @Test
    void showsPiersFromTheFile() {
        Pier harbour = piers.find("HARBOUR").orElseThrow();

        assertEquals(HARBOUR, harbour.details());
        assertTrue(harbour.open());
        assertTrue(harbour.isFromConfig());
        assertEquals(Map.of("faction", "crown"), harbour.data());
        assertEquals(new BigDecimal("5"), harbour.finalLossChance(Set.of()));
        assertEquals(List.of(harbour), piers.all());
        assertTrue(storage.piers.isEmpty(), "nothing is saved until something changes");
    }

    @Test
    void createsUpdatesAndRemovesPluginPiers() {
        Pier docks = piers.create("SkysongPolitics", "Docks", DOCKS);

        assertEquals("docks", docks.id());
        assertEquals("SkysongPolitics", docks.source());
        assertTrue(docks.open());
        assertEquals(BigDecimal.ZERO, docks.lossChance());
        assertEquals(List.of("harbour", "docks"), piers.all().stream().map(Pier::id).toList());
        assertEquals(DOCKS, storage.piers.get("docks").details());

        PierDetails renamed = new PierDetails("<white>Old Docks", "BARREL", List.of());
        assertEquals(renamed, piers.update("docks", renamed).details());
        piers.remove("docks");

        assertTrue(piers.find("docks").isEmpty());
        assertTrue(storage.piers.isEmpty());
        assertEquals(List.of("none -> docks", "docks -> docks", "docks -> none"), changes);
    }

    @Test
    void refusesClashingAndBadPiers() {
        assertEquals("piers.exists", assertThrows(PierException.class,
                () -> piers.create("SkysongPolitics", "harbour", DOCKS)).messageKey());
        piers.create("SkysongPolitics", "docks", DOCKS);
        assertEquals("piers.exists", assertThrows(PierException.class,
                () -> piers.create("Other", "docks", DOCKS)).messageKey());
        assertEquals("piers.invalid-id", assertThrows(PierException.class,
                () -> piers.create("SkysongPolitics", "the docks", DOCKS)).messageKey());
        assertEquals("piers.invalid-owner", assertThrows(PierException.class,
                () -> piers.create("config", "pier", DOCKS)).messageKey());
        assertEquals("piers.from-config", assertThrows(PierException.class,
                () -> piers.update("harbour", DOCKS)).messageKey());
        assertEquals("piers.from-config", assertThrows(PierException.class, () -> piers.remove("harbour")).messageKey());
        assertEquals("piers.unknown", assertThrows(PierException.class, () -> piers.setOpen("moon", false)).messageKey());
    }

    @Test
    void savesChangesToFilePiersOnTopOfTheFile() {
        piers.setOpen("harbour", false);
        piers.setData("harbour", "faction", "corsairs");
        piers.setData("harbour", "guards", "4");
        piers.setLossChance("harbour", new BigDecimal("10"));

        Pier harbour = piers.find("harbour").orElseThrow();
        assertFalse(harbour.open());
        assertEquals(Map.of("faction", "corsairs", "guards", "4"), harbour.data());
        assertEquals(new BigDecimal("10"), harbour.lossChance());
        assertTrue(piers.open().isEmpty());
        PierRepository.Row row = storage.piers.get("harbour");
        assertEquals(Pier.CONFIG, row.source());
        assertEquals(null, row.details(), "the file keeps the name, icon and description");

        piers.setData("harbour", "faction", null);
        assertEquals(Map.of("faction", "crown", "guards", "4"), piers.find("harbour").orElseThrow().data(),
                "removing a value shows the file's again");
        assertEquals(5, changes.size());
    }

    @Test
    void addsReplacesAndRemovesLossModifiers() {
        piers.setLossModifier("harbour", points("guards", 5, "contraband"));
        piers.setLossModifier("harbour", points("guards", 10, "contraband"));
        Pier harbour = piers.find("harbour").orElseThrow();

        assertEquals(new BigDecimal("5"), harbour.finalLossChance(Set.of()));
        assertEquals(new BigDecimal("15"), harbour.finalLossChance(Set.of("contraband")));
        assertEquals(1, storage.modifiers.size());

        assertTrue(piers.removeLossModifier("harbour", "GUARDS"));
        assertFalse(piers.removeLossModifier("harbour", "guards"));
        assertEquals(new BigDecimal("5"), piers.find("harbour").orElseThrow().finalLossChance(Set.of("contraband")));
        assertTrue(storage.modifiers.isEmpty());
    }

    @Test
    void aModifierFromTheFileCanBeReplacedButNotRemoved() {
        piers.setLossModifier("harbour", points("rough_waters", 1, null));
        assertEquals(new BigDecimal("3"), piers.find("harbour").orElseThrow().finalLossChance(Set.of()));

        assertTrue(piers.removeLossModifier("harbour", "rough_waters"), "removes the replacement");
        assertEquals("piers.modifier-from-config", assertThrows(PierException.class,
                () -> piers.removeLossModifier("harbour", "rough_waters")).messageKey());
    }

    @Test
    void refusesChancesOutsideZeroToHundred() {
        assertEquals("piers.invalid-chance", assertThrows(PierException.class,
                () -> piers.setLossChance("harbour", new BigDecimal("101"))).messageKey());
        assertThrows(PierException.class, () -> piers.setLossChance("harbour", new BigDecimal("-1")));
    }

    @Test
    void keepsEverythingAcrossARestart() {
        piers.create("SkysongPolitics", "docks", DOCKS);
        piers.setLossModifier("docks", points("guards", 5, "contraband"));
        piers.setOpen("harbour", false);

        start(file(harbour()));

        assertFalse(piers.find("harbour").orElseThrow().open());
        assertEquals(new BigDecimal("5"), piers.find("docks").orElseThrow().finalLossChance(Set.of("contraband")));
        assertEquals(List.of("harbour", "docks"), piers.all().stream().map(Pier::id).toList());
    }

    @Test
    void reloadTellsWhatChanged() {
        piers.create("SkysongPolitics", "docks", DOCKS);
        changes.clear();

        piers.reload(file(new ConfiguredPier("cove", DOCKS, Map.of(), BigDecimal.ZERO, Map.of())));

        assertEquals(List.of("harbour -> none", "none -> cove"), changes, "the unchanged plugin pier isn't reported");
        assertTrue(piers.find("docks").isPresent(), "plugin piers don't depend on the file");
    }

    @Test
    void aPluginPierKeepsItsIdWhenTheFileAddsTheSameOne() {
        piers.create("SkysongPolitics", "docks", DOCKS);

        piers.reload(file(harbour(), new ConfiguredPier("docks", HARBOUR, Map.of(), BigDecimal.ONE, Map.of())));

        assertEquals("SkysongPolitics", piers.find("docks").orElseThrow().source());
        assertEquals(1, warnings.size());
    }

    @Test
    void changesToAPierDeletedFromTheFileComeBackWithIt() {
        piers.setOpen("harbour", false);
        piers.reload(PierSettings.NONE);
        assertTrue(piers.find("harbour").isEmpty());
        assertEquals("piers.unknown", assertThrows(PierException.class, () -> piers.setOpen("harbour", true)).messageKey());

        piers.reload(file(harbour()));
        assertFalse(piers.find("harbour").orElseThrow().open());
    }
}
