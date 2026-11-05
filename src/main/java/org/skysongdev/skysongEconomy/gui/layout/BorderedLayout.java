package org.skysongdev.skysongEconomy.gui.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * The layout every list menu shares: a 6-row chest with a border around it.
 * <pre>
 *   row 0:  B B B B H B B B B      H = header (what you are looking at)
 *   row 1:  B c c c c c c c S      c = content, 28 slots
 *   row 2:  B c c c c c c c S      S = side buttons (right column), 4 slots
 *   row 3:  B c c c c c c c S
 *   row 4:  B c c c c c c c S
 *   row 5:  B B B P K N B B B      P = previous page, K = back, N = next page
 * </pre>
 * Slots are numbered like a chest: left to right, top to bottom, starting at 0.
 */
public final class BorderedLayout {
    public static final int ROWS = 6;
    public static final int COLUMNS = 9;
    public static final int SIZE = ROWS * COLUMNS;

    public static final int HEADER = 4;
    public static final int PREVIOUS_PAGE = 48;
    public static final int BACK = 49;
    public static final int NEXT_PAGE = 50;

    private static final List<Integer> CONTENT = slots(1, 4, 1, 7);
    private static final List<Integer> SIDE = slots(1, 4, 8, 8);

    private BorderedLayout() {
    }

    /** @return the 28 content slots, in reading order */
    public static List<Integer> content() {
        return CONTENT;
    }

    /** @return the 4 slots of the right column, top to bottom */
    public static List<Integer> side() {
        return SIDE;
    }

    /** @return whether the slot is part of the border (everything that isn't content or side) */
    public static boolean isBorder(int slot) {
        return slot >= 0 && slot < SIZE && !CONTENT.contains(slot) && !SIDE.contains(slot);
    }

    /** @return the slot at a row and column, both from 0 */
    public static int slot(int row, int column) {
        if (row < 0 || row >= ROWS || column < 0 || column >= COLUMNS) {
            throw new IllegalArgumentException("No slot at row " + row + ", column " + column);
        }
        return row * COLUMNS + column;
    }

    private static List<Integer> slots(int fromRow, int toRow, int fromColumn, int toColumn) {
        List<Integer> slots = new ArrayList<>();
        for (int row = fromRow; row <= toRow; row++) {
            for (int column = fromColumn; column <= toColumn; column++) {
                slots.add(slot(row, column));
            }
        }
        return List.copyOf(slots);
    }
}
