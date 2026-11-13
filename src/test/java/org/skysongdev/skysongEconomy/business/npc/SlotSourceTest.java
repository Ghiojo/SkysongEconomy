package org.skysongdev.skysongEconomy.business.npc;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotSourceTest {
    @Test
    void aSkillsSlotsAreSavedAsSkillColonId() {
        SlotSource source = SlotSource.skill("Martial");

        assertEquals("skill:martial", source.key());
        assertTrue(source.isSkill());
        assertEquals(Optional.of(source), SlotSource.parse("skill:martial"));
    }

    @Test
    void theIdMayHoldColons() {
        assertEquals(Optional.of(new SlotSource("module", "kennel:2")), SlotSource.parse("module:kennel:2"));
    }

    @Test
    void anythingElseIsNotASource() {
        assertEquals(Optional.empty(), SlotSource.parse("martial"));
        assertEquals(Optional.empty(), SlotSource.parse(":martial"));
        assertEquals(Optional.empty(), SlotSource.parse("skill:"));
        assertThrows(IllegalArgumentException.class, () -> new SlotSource("", "martial"));
        assertThrows(IllegalArgumentException.class, () -> new SlotSource("a:b", "martial"));
    }
}
