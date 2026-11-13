package org.skysongdev.skysongEconomy.business.npc;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessGroupRefTest {
    @Test
    void isWrittenAsEconomyBusinessSlashGroup() {
        BusinessGroupRef ref = new BusinessGroupRef("smithy", "workers");

        assertEquals("smithy/workers", ref.id());
        assertEquals("economy:smithy/workers", ref.toString());
    }

    @Test
    void readsBackWhatItWrote() {
        BusinessGroupRef ref = new BusinessGroupRef("old-town_smithy", "vip-customers");

        assertEquals(Optional.of(ref), BusinessGroupRef.parseId(ref.id()));
    }

    @Test
    void anythingElseIsNotABusinessGroup() {
        assertEquals(Optional.empty(), BusinessGroupRef.parseId("smithy"));
        assertEquals(Optional.empty(), BusinessGroupRef.parseId("/workers"));
        assertEquals(Optional.empty(), BusinessGroupRef.parseId("smithy/"));
        assertEquals(Optional.empty(), BusinessGroupRef.parseId("smithy/workers/extra"));
        assertEquals(Optional.empty(), BusinessGroupRef.parseId(""));
    }

    @Test
    void refusesPartsThatWouldBeAmbiguous() {
        assertThrows(IllegalArgumentException.class, () -> new BusinessGroupRef("", "workers"));
        assertThrows(IllegalArgumentException.class, () -> new BusinessGroupRef("smithy", ""));
        assertThrows(IllegalArgumentException.class, () -> new BusinessGroupRef("smi/thy", "workers"));
    }
}
