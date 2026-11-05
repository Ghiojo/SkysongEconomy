package org.skysongdev.skysongEconomy.prices;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * The {@code /se ledger} price database: each item's base value, the {@code {base}} of the
 * prices that sell or buy it.
 * <p>
 * A base value is an expression too, so a sword can follow the price of its steel:
 * {@code {item:steel_ingot} * 2 + 1}. It can't use {@code {base}}, can only name items that
 * have a base value, and can't loop back to itself; an item other items use can't be removed.
 * Values are worked out when asked, so a change to steel changes the sword at once.
 * <p>
 * Not thread-safe: use it on the main thread.
 */
public final class PriceDatabase {
    private final BaseValueRepository repository;
    private final Map<String, PriceExpression> expressions = new HashMap<>();

    public PriceDatabase(BaseValueRepository repository) {
        this.repository = repository;
    }

    /**
     * Loads every saved base value. Entries that can't be read are skipped and reported.
     *
     * @param warnings told about each skipped entry
     */
    public void loadAll(Consumer<String> warnings) {
        expressions.clear();
        repository.loadAll().forEach((materialId, text) -> {
            try {
                expressions.put(normalize(materialId), PriceExpression.parse(text));
            } catch (PriceException e) {
                warnings.accept("The price database entry of " + materialId + " (" + text + ") is ignored: " + e.placeholders());
            }
        });
    }

    /** @return the item's base value expression */
    public Optional<PriceExpression> expression(String materialId) {
        return Optional.ofNullable(expressions.get(normalize(materialId)));
    }

    /** @return every item's expression, by material id in alphabetical order */
    public Map<String, PriceExpression> all() {
        return new TreeMap<>(expressions);
    }

    /**
     * @return the item's base value, exact; empty when it has none
     * @throws PriceException when it can't be worked out, e.g. it divides by zero
     */
    public Optional<BigDecimal> value(String materialId) {
        String id = normalize(materialId);
        if (!expressions.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(new ItemValues().item(id));
    }

    /**
     * @param base the value of {@code {base}}, or {@code null} where it has no meaning
     * @return where an expression gets {@code {base}} and other items' base values from
     */
    public PriceContext context(@Nullable BigDecimal base) {
        ItemValues items = new ItemValues();
        return base == null ? items : new Priced(base, items);
    }

    /**
     * Sets an item's base value.
     *
     * @return what it is worth now
     * @throws PriceException when the expression is invalid, uses {@code {base}}, names an
     *                        item without a base value, or loops back to this item
     */
    public BigDecimal set(String materialId, String text) {
        String id = normalize(materialId);
        PriceExpression expression = PriceExpression.parse(text);
        if (expression.usesBase()) {
            throw new PriceException("prices.base-in-database", Map.of("material", id));
        }
        for (String item : expression.items()) {
            String used = normalize(item);
            if (!used.equals(id) && !expressions.containsKey(used)) {
                throw new PriceException("prices.unknown-item", Map.of("material", used));
            }
        }
        PriceExpression previous = expressions.put(id, expression);
        try {
            requireNoLoop(id);
            BigDecimal value = new ItemValues().item(id);
            repository.save(id, expression.text(), value);
            return value;
        } catch (PriceException e) {
            if (previous == null) {
                expressions.remove(id);
            } else {
                expressions.put(id, previous);
            }
            throw e;
        }
    }

    /**
     * Removes an item's base value.
     *
     * @return whether it had one
     * @throws PriceException {@code prices.still-used} when another item's value uses it
     */
    public boolean remove(String materialId) {
        String id = normalize(materialId);
        if (!expressions.containsKey(id)) {
            return false;
        }
        List<String> users = expressions.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(id) && uses(entry.getValue(), id))
                .map(Map.Entry::getKey).sorted().toList();
        if (!users.isEmpty()) {
            throw new PriceException("prices.still-used", Map.of("material", id, "users", String.join(", ", users)));
        }
        expressions.remove(id);
        repository.delete(id);
        return true;
    }

    private static boolean uses(PriceExpression expression, String id) {
        return expression.items().stream().anyMatch(item -> normalize(item).equals(id));
    }

    /** Depth-first search from {@code start} through {@code {item:...}} references. */
    private void requireNoLoop(String start) {
        Deque<String> path = new ArrayDeque<>();
        visit(start, start, path);
    }

    private void visit(String start, String current, Deque<String> path) {
        path.addLast(current);
        PriceExpression expression = expressions.get(current);
        if (expression != null) {
            for (String item : expression.items()) {
                String next = normalize(item);
                if (next.equals(start)) {
                    path.addLast(next);
                    throw new PriceException("prices.loop", Map.of("path", String.join(" -> ", path)));
                }
                if (!path.contains(next)) {
                    visit(start, next, path);
                }
            }
        }
        path.removeLast();
    }

    private static String normalize(String materialId) {
        return materialId.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * Works out item base values for one evaluation: remembers them, and guards against loops
     * in data edited by hand. Base values have no {@code {base}}.
     */
    private final class ItemValues implements PriceContext {
        private final Map<String, BigDecimal> known = new HashMap<>();
        private final Deque<String> inProgress = new ArrayDeque<>();

        @Override
        public BigDecimal base() {
            throw new PriceException("prices.no-base", Map.of());
        }

        @Override
        public BigDecimal item(String materialId) {
            String id = normalize(materialId);
            BigDecimal cached = known.get(id);
            if (cached != null) {
                return cached;
            }
            PriceExpression expression = expressions.get(id);
            if (expression == null) {
                throw new PriceException("prices.unknown-item", Map.of("material", id));
            }
            if (inProgress.contains(id)) {
                throw new PriceException("prices.loop", Map.of("path", String.join(" -> ", inProgress) + " -> " + id));
            }
            inProgress.addLast(id);
            BigDecimal value = expression.evaluate(this);
            inProgress.removeLast();
            known.put(id, value);
            return value;
        }
    }

    /** A priced expression's context: the given {@code {base}}, and items from the database. */
    private record Priced(BigDecimal base, ItemValues items) implements PriceContext {
        @Override
        public BigDecimal item(String materialId) {
            return items.item(materialId);
        }
    }
}
