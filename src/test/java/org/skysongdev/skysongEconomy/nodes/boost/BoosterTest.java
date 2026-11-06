package org.skysongdev.skysongEconomy.nodes.boost;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoosterTest {
    @Test
    void matchesItsTypeIgnoringCaseAndSpaces() {
        Booster booster = new Booster(" Mining ", BoosterEffect.NONE);
        assertEquals("mining", booster.type());
        assertTrue(booster.worksOn("mining"));
        assertTrue(booster.worksOn("MINING "));
        assertFalse(booster.worksOn("crop"));
    }
}
