package org.skysongdev.skysongEconomy.api.business;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessInfoTest {
    @Test
    void theOlderConstructorMakesABusinessWithNoBlock() {
        BusinessInfo info = new BusinessInfo("docks", "trading", "Docks", 1, Optional.empty(), List.of());

        assertEquals(Optional.empty(), info.block());
    }

    @Test
    void refusesAMissingBlockOptional() {
        assertThrows(NullPointerException.class,
                () -> new BusinessInfo("docks", "trading", "Docks", 1, Optional.empty(), List.of(), null));
    }

    @Test
    void aBusinessBlockKnowsItsCoordinates() {
        BusinessBlock block = new BusinessBlock("world", 3, 64, -7);

        assertTrue(block.isAt("world", 3, 64, -7));
        assertFalse(block.isAt("world_nether", 3, 64, -7));
        assertFalse(block.isAt("world", 3, 65, -7));
        assertEquals("world 3 64 -7", block.toString());
        assertThrows(NullPointerException.class, () -> new BusinessBlock(null, 0, 0, 0));
    }
}
