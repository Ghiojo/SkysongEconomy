package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.prices.PriceContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * What one import delivers: fixed items, items with random amounts, and weighted rolls, in any
 * mix.
 */
public record Bundle(List<BundlePart> parts) {
    public Bundle {
        parts = List.copyOf(parts);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("A bundle needs at least one part");
        }
    }

    /** @return the items of one delivery, one entry per item with amounts added up */
    public List<ItemAmount> roll(RandomGenerator random) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (BundlePart part : parts) {
            for (ItemAmount item : part.roll(random)) {
                totals.merge(item.materialId(), item.amount(), Integer::sum);
            }
        }
        List<ItemAmount> result = new ArrayList<>();
        totals.forEach((id, amount) -> result.add(new ItemAmount(id, amount)));
        return result;
    }

    /**
     * @return the bundle's {@code {base}}: its fixed items' base values plus the average value
     *         of each random part
     */
    public BigDecimal baseValue(PriceContext items) {
        BigDecimal total = BigDecimal.ZERO;
        for (BundlePart part : parts) {
            total = total.add(part.averageValue(items));
        }
        return total;
    }

    /** @return every material id the bundle can deliver */
    public Set<String> materialIds() {
        Set<String> ids = new LinkedHashSet<>();
        parts.forEach(part -> ids.addAll(part.materialIds()));
        return ids;
    }
}
