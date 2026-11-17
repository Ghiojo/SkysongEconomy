package org.skysongdev.skysongEconomy.upkeep;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemMarkTest {
    @Test
    void anUnmaintainedNpcsUnmarkedItemGetsMarked() {
        assertEquals(ItemMark.MARK, ItemMark.decide(UpkeepState.UNMAINTAINED, false, false));
    }

    @Test
    void anUnmaintainedNpcsMarkedItemIsLeftAlone() {
        assertEquals(ItemMark.NONE, ItemMark.decide(UpkeepState.UNMAINTAINED, true, true));
    }

    @Test
    void aMarkedItemThatLostItsTraitRepairsTheNpc() {
        assertEquals(ItemMark.REPAIR, ItemMark.decide(UpkeepState.UNMAINTAINED, false, true));
    }

    @Test
    void aTraitAddedByHandGetsItsTag() {
        assertEquals(ItemMark.MARK, ItemMark.decide(UpkeepState.UNMAINTAINED, true, false));
    }

    @Test
    void aMaintainedNpcsItemLosesTheTraitAndTheTag() {
        assertEquals(ItemMark.UNMARK, ItemMark.decide(UpkeepState.MAINTAINED, true, true));
        assertEquals(ItemMark.UNMARK, ItemMark.decide(UpkeepState.MAINTAINED, false, true));
        assertEquals(ItemMark.NONE, ItemMark.decide(UpkeepState.MAINTAINED, false, false));
    }

    @Test
    void anItemWhoseNpcHasNoUpkeepIsCleanedUp() {
        assertEquals(ItemMark.UNMARK, ItemMark.decide(null, true, true));
        assertEquals(ItemMark.NONE, ItemMark.decide(null, false, false));
    }
}
