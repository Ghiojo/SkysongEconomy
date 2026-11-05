package org.skysongdev.skysongEconomy.gui.layout;

import java.util.List;

/**
 * One page of a list shown in a menu.
 *
 * @param items  the items on this page
 * @param number this page, from 0
 * @param count  how many pages there are, at least 1
 */
public record Page<T>(List<T> items, int number, int count) {
    public Page {
        items = List.copyOf(items);
    }

    /**
     * Cuts a list into pages and returns one of them. A page number past the end gives the last
     * page, so a list that shrank while a player looked at it still shows something.
     *
     * @param pageSize how many items fit on a page, at least 1
     */
    public static <T> Page<T> of(List<T> all, int pageSize, int number) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be at least 1: " + pageSize);
        }
        int count = Math.max(1, (all.size() + pageSize - 1) / pageSize);
        int clamped = Math.max(0, Math.min(number, count - 1));
        int from = clamped * pageSize;
        return new Page<>(all.subList(from, Math.min(all.size(), from + pageSize)), clamped, count);
    }

    public boolean hasPrevious() {
        return number > 0;
    }

    public boolean hasNext() {
        return number < count - 1;
    }
}
