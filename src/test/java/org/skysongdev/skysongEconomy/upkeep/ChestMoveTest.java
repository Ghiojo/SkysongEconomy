package org.skysongdev.skysongEconomy.upkeep;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChestMoveTest {
    @Test
    void anyoneMayPutInItemsThatPayTheCost() {
        assertTrue(ChestMove.PUT_IN.allowed(false, true));
        assertTrue(ChestMove.PUT_IN.allowed(true, true));
    }

    @Test
    void nobodyMayPutInItemsThatDoNotPayTheCost() {
        assertFalse(ChestMove.PUT_IN.allowed(false, false));
        assertFalse(ChestMove.PUT_IN.allowed(true, false));
        assertFalse(ChestMove.SWAP.allowed(true, false));
    }

    @Test
    void onlyPlayersWhoMayEditTakeItemsOut() {
        assertFalse(ChestMove.TAKE_OUT.allowed(false, true));
        assertTrue(ChestMove.TAKE_OUT.allowed(true, true));
        assertFalse(ChestMove.SWAP.allowed(false, true));
        assertTrue(ChestMove.SWAP.allowed(true, true));
        assertFalse(ChestMove.OTHER.allowed(false, true));
        assertTrue(ChestMove.OTHER.allowed(true, true));
    }

    @Test
    void movesThatLeaveTheChestAloneAreAlwaysAllowed() {
        assertTrue(ChestMove.NONE.allowed(false, false));
    }
}
