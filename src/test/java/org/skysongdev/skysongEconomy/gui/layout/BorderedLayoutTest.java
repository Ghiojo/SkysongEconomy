package org.skysongdev.skysongEconomy.gui.layout;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderedLayoutTest {
    @Test
    void contentIsTheInnerFourRowsBySevenColumns() {
        List<Integer> content = BorderedLayout.content();

        assertEquals(28, content.size());
        assertEquals(10, content.get(0));
        assertEquals(16, content.get(6));
        assertEquals(19, content.get(7), "the second row starts after the side and border columns");
        assertEquals(43, content.get(27));
    }

    @Test
    void sideIsTheRightColumnBetweenTheBorders() {
        assertEquals(List.of(17, 26, 35, 44), BorderedLayout.side());
    }

    @Test
    void everySlotIsExactlyOneOfBorderContentOrSide() {
        Set<Integer> seen = new HashSet<>();
        IntStream.range(0, BorderedLayout.SIZE).forEach(slot -> {
            int kinds = (BorderedLayout.isBorder(slot) ? 1 : 0)
                    + (BorderedLayout.content().contains(slot) ? 1 : 0)
                    + (BorderedLayout.side().contains(slot) ? 1 : 0);
            assertEquals(1, kinds, "slot " + slot);
            seen.add(slot);
        });
        assertEquals(54, seen.size());
    }

    @Test
    void headerAndNavigationSitOnTheBorder() {
        assertTrue(BorderedLayout.isBorder(BorderedLayout.HEADER));
        assertTrue(BorderedLayout.isBorder(BorderedLayout.PREVIOUS_PAGE));
        assertTrue(BorderedLayout.isBorder(BorderedLayout.BACK));
        assertTrue(BorderedLayout.isBorder(BorderedLayout.NEXT_PAGE));
        assertFalse(BorderedLayout.isBorder(54));
    }

    @Test
    void slotCountsRowsAndColumnsFromZero() {
        assertEquals(0, BorderedLayout.slot(0, 0));
        assertEquals(20, BorderedLayout.slot(2, 2));
        assertEquals(53, BorderedLayout.slot(5, 8));
        assertThrows(IllegalArgumentException.class, () -> BorderedLayout.slot(6, 0));
        assertThrows(IllegalArgumentException.class, () -> BorderedLayout.slot(0, 9));
    }
}
