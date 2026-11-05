package org.skysongdev.skysongEconomy.business.access;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessGroupTest {
    private static final CharacterRef ALICE = new CharacterRef(UUID.randomUUID(), "Alice");

    @Test
    void changesMakeNewGroups() {
        AccessGroup group = new AccessGroup("deal", Set.of(), BigDecimal.ONE, Set.of());

        AccessGroup changed = group.withPermission(Permission.TRAIN, true).withMember(ALICE, true);

        assertFalse(group.allows(Permission.TRAIN), "the original is unchanged");
        assertTrue(changed.allows(Permission.TRAIN));
        assertEquals(Set.of(ALICE), changed.members());
        assertFalse(changed.withPermission(Permission.TRAIN, false).allows(Permission.TRAIN));
    }

    @Test
    void refusesNegativePricesAndMembersOfBuiltInGroups() {
        assertThrows(IllegalArgumentException.class,
                () -> new AccessGroup("deal", Set.of(), new BigDecimal("-0.1"), Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new AccessGroup(AccessGroup.EVERYONE, Set.of(), BigDecimal.ONE, Set.of(ALICE)));
    }
}
