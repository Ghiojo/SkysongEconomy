package org.skysongdev.skysongEconomy.items;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoredItemsTest {
    private static final StoredItems TWO = StoredItems.of(List.of(new byte[] {1, 2, 3}, new byte[] {9}));

    @Test
    void survivesTheTripThroughOneColumn() {
        StoredItems read = StoredItems.fromBytes(TWO.toBytes());

        assertEquals(TWO, read);
        assertEquals(TWO.hashCode(), read.hashCode());
        assertArrayEquals(new byte[] {1, 2, 3}, read.stacks().get(0));
        assertArrayEquals(new byte[] {9}, read.stacks().get(1));
    }

    @Test
    void isNotChangedThroughTheArraysItHandsOut() {
        byte[] stack = {5};
        StoredItems items = StoredItems.of(List.of(stack));
        stack[0] = 6;
        items.stacks().get(0)[0] = 7;

        assertArrayEquals(new byte[] {5}, items.stacks().get(0));
    }

    @Test
    void comparesByContent() {
        assertEquals(TWO, StoredItems.of(List.of(new byte[] {1, 2, 3}, new byte[] {9})));
        assertNotEquals(TWO, StoredItems.of(List.of(new byte[] {1, 2, 3})));
    }

    @Test
    void refusesNoStacksAndBrokenBytes() {
        assertThrows(IllegalArgumentException.class, () -> StoredItems.of(List.of()));
        assertThrows(IllegalArgumentException.class, () -> StoredItems.of(List.of(new byte[0])));
        byte[] bytes = TWO.toBytes();
        assertThrows(IllegalArgumentException.class, () -> StoredItems.fromBytes(Arrays.copyOf(bytes, bytes.length - 1)));
        assertThrows(IllegalArgumentException.class, () -> StoredItems.fromBytes(Arrays.copyOf(bytes, bytes.length + 1)));
        assertThrows(IllegalArgumentException.class, () -> StoredItems.fromBytes(new byte[] {0, 0}));
    }
}
