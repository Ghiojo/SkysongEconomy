package org.skysongdev.skysongEconomy.prices;

import org.skysongdev.skysongEconomy.trade.GoodTypeIndex;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Read-only lookups of base values and good types, behind the public
 * {@link org.skysongdev.skysongEconomy.api.items.ItemCatalog}. Values are worked out on every
 * call, so it always shows the current ones. Material ids and good types ignore case and
 * surrounding spaces; everything returned is an immutable copy. Kept free of Bukkit classes so
 * it can be tested on its own.
 * <p>
 * Not thread-safe: use it on the main thread, like the classes it reads.
 */
public final class ItemLookup {
    private final PriceDatabase prices;
    private final PriceSettings settings;
    private final GoodTypeIndex goodTypes;

    /**
     * @param prices      every item's base value
     * @param settings    how many decimals prices keep
     * @param goodTypes   every item's good types
     */
    public ItemLookup(PriceDatabase prices, PriceSettings settings, GoodTypeIndex goodTypes) {
        this.prices = Objects.requireNonNull(prices, "prices");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.goodTypes = Objects.requireNonNull(goodTypes, "goodTypes");
    }

    /** @return the base value truncated to the configured decimals; empty when there is none or it can't be worked out */
    public Optional<BigDecimal> baseValue(String materialId) {
        Objects.requireNonNull(materialId, "materialId");
        try {
            return prices.value(materialId).map(settings::truncate);
        } catch (PriceException e) {
            return Optional.empty();
        }
    }

    /** @return every base value that can be worked out, by lower-case material id, alphabetically */
    public SortedMap<String, BigDecimal> baseValues() {
        SortedMap<String, BigDecimal> values = new TreeMap<>();
        prices.all().keySet().forEach(id -> baseValue(id).ifPresent(value -> values.put(id, value)));
        return Collections.unmodifiableSortedMap(values);
    }

    /** @return how many decimals prices keep */
    public int decimals() {
        return settings.decimals();
    }

    /** @return the item's good types, alphabetically */
    public Set<String> goodTypes(String materialId) {
        String id = normalize(Objects.requireNonNull(materialId, "materialId"));
        SortedSet<String> types = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        goodTypes.all().forEach((item, itemTypes) -> {
            if (normalize(item).equals(id)) {
                types.addAll(itemTypes);
            }
        });
        return Collections.unmodifiableSortedSet(types);
    }

    /** @return whether the item has that good type */
    public boolean hasGoodType(String materialId, String goodType) {
        String type = normalize(Objects.requireNonNull(goodType, "goodType"));
        return goodTypes(materialId).stream().anyMatch(each -> normalize(each).equals(type));
    }

    /** @return every good type at least one item has, alphabetically */
    public SortedSet<String> allGoodTypes() {
        SortedSet<String> types = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        goodTypes.all().values().forEach(types::addAll);
        return Collections.unmodifiableSortedSet(types);
    }

    /** @return the lower-case material ids that have that good type, alphabetically */
    public SortedSet<String> itemsOf(String goodType) {
        String type = normalize(Objects.requireNonNull(goodType, "goodType"));
        SortedSet<String> items = new TreeSet<>();
        for (Map.Entry<String, Set<String>> entry : goodTypes.all().entrySet()) {
            if (entry.getValue().stream().anyMatch(each -> normalize(each).equals(type))) {
                items.add(normalize(entry.getKey()));
            }
        }
        return Collections.unmodifiableSortedSet(items);
    }

    private static String normalize(String text) {
        return text.strip().toLowerCase(Locale.ROOT);
    }
}
