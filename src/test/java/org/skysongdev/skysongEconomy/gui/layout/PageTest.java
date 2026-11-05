package org.skysongdev.skysongEconomy.gui.layout;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageTest {
    private static final List<Integer> SIXTY = IntStream.range(0, 60).boxed().toList();

    @Test
    void cutsAListIntoPages() {
        Page<Integer> first = Page.of(SIXTY, 28, 0);
        Page<Integer> last = Page.of(SIXTY, 28, 2);

        assertEquals(3, first.count());
        assertEquals(28, first.items().size());
        assertEquals(0, first.items().get(0));
        assertFalse(first.hasPrevious());
        assertTrue(first.hasNext());
        assertEquals(List.of(56, 57, 58, 59), last.items());
        assertTrue(last.hasPrevious());
        assertFalse(last.hasNext());
    }

    @Test
    void pagesPastTheEndShowTheLastPage() {
        Page<Integer> page = Page.of(SIXTY, 28, 9);

        assertEquals(2, page.number());
        assertEquals(0, Page.of(SIXTY, 28, -1).number());
    }

    @Test
    void anEmptyListHasOneEmptyPage() {
        Page<Integer> page = Page.of(List.of(), 28, 0);

        assertEquals(1, page.count());
        assertTrue(page.items().isEmpty());
        assertFalse(page.hasNext());
    }

    @Test
    void pageSizeMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> Page.of(SIXTY, 0, 0));
    }
}
