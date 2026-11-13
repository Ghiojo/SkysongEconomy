package org.skysongdev.skysongEconomy.business.scale;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A table of values by level or by tier, such as {@code {by: level, 1: proficient, 4: adept}}.
 * A row applies from its number upward, until a later row takes over: that table gives
 * proficient at levels 1 to 3 and adept from level 4. Below its first row a table gives nothing.
 *
 * @param <T> what each row holds
 */
public final class Steps<T> {
    private final Basis basis;
    private final TreeMap<Integer, T> rows;

    /**
     * @param basis which number the rows follow
     * @param rows  the value from each number upward; not empty, numbers 1 or more
     * @throws IllegalArgumentException when there is no row, or a row's number is below 1
     */
    public Steps(Basis basis, Map<Integer, T> rows) {
        this.basis = Objects.requireNonNull(basis, "basis");
        this.rows = new TreeMap<>(rows);
        if (this.rows.isEmpty()) {
            throw new IllegalArgumentException("A table needs at least one row");
        }
        if (this.rows.firstKey() < 1) {
            throw new IllegalArgumentException("Rows start at 1, not " + this.rows.firstKey());
        }
        this.rows.values().forEach(value -> Objects.requireNonNull(value, "row value"));
    }

    /** @return which number the rows follow */
    public Basis basis() {
        return basis;
    }

    /** @return the rows by their number, lowest first, unmodifiable */
    public Map<Integer, T> rows() {
        return Collections.unmodifiableMap(rows);
    }

    /** @return the row that applies at that scale: the highest one reached; empty below the first row */
    public Optional<T> at(BusinessScale scale) {
        Map.Entry<Integer, T> row = rows.floorEntry(scale.value(basis));
        return row == null ? Optional.empty() : Optional.of(row.getValue());
    }

    /**
     * @return every row reached at that scale, lowest first; for tables whose rows add up
     *         instead of replacing each other
     */
    public List<T> reached(BusinessScale scale) {
        return List.copyOf(rows.headMap(scale.value(basis), true).values());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Steps<?> steps && steps.basis == basis && steps.rows.equals(rows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(basis, rows);
    }

    @Override
    public String toString() {
        return "{by: " + basis.key() + ", " + rows + "}";
    }
}
