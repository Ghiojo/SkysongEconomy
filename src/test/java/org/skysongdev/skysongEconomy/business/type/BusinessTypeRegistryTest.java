package org.skysongdev.skysongEconomy.business.type;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessTypeRegistryTest {
    private static final BusinessType GATHERING = new BusinessType("gathering", "Gathering", Set.of("mining"), false);
    private static final BusinessType SERVICE = new BusinessType("service", "Service", Set.of("martial"), true);

    @Test
    void findsTypesByIdAndBySkill() {
        BusinessTypeRegistry registry = new BusinessTypeRegistry(List.of(GATHERING, SERVICE));

        assertEquals(GATHERING, registry.find("gathering").orElseThrow());
        assertEquals(SERVICE, registry.typeOfSkill("martial").orElseThrow());
        assertTrue(registry.find("crafting").isEmpty());
        assertTrue(registry.typeOfSkill("cooking").isEmpty());
        assertEquals(List.of(GATHERING, SERVICE), registry.all());
    }

    @Test
    void refusesDuplicateIdsAndSharedSkills() {
        assertThrows(IllegalArgumentException.class, () -> new BusinessTypeRegistry(List.of(GATHERING, GATHERING)));
        BusinessType alsoMining = new BusinessType("other", "Other", Set.of("mining"), false);
        assertThrows(IllegalArgumentException.class, () -> new BusinessTypeRegistry(List.of(GATHERING, alsoMining)));
    }
}
