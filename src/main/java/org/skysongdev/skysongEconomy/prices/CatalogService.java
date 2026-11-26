package org.skysongdev.skysongEconomy.prices;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.items.ItemCatalog;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Function;

/**
 * The {@link ItemCatalog} other plugins read. Values and good types come from {@link ItemLookup};
 * this class only adds reading the material id of a stack.
 */
public final class CatalogService implements ItemCatalog {
    private final ItemLookup lookup;
    private final Function<ItemStack, Optional<String>> materialIds;

    /**
     * @param lookup      base values and good types
     * @param materialIds reads the Skysong material id of a stack that isn't null
     */
    public CatalogService(ItemLookup lookup, Function<ItemStack, Optional<String>> materialIds) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
        this.materialIds = Objects.requireNonNull(materialIds, "materialIds");
    }

    @Override
    public Optional<BigDecimal> baseValue(String materialId) {
        return lookup.baseValue(materialId);
    }

    @Override
    public SortedMap<String, BigDecimal> baseValues() {
        return lookup.baseValues();
    }

    @Override
    public int decimals() {
        return lookup.decimals();
    }

    @Override
    public Set<String> goodTypes(String materialId) {
        return lookup.goodTypes(materialId);
    }

    @Override
    public boolean hasGoodType(String materialId, String goodType) {
        return lookup.hasGoodType(materialId, goodType);
    }

    @Override
    public SortedSet<String> allGoodTypes() {
        return lookup.allGoodTypes();
    }

    @Override
    public SortedSet<String> itemsOf(String goodType) {
        return lookup.itemsOf(goodType);
    }

    @Override
    public Optional<String> materialId(@Nullable ItemStack item) {
        return item == null ? Optional.empty() : materialIds.apply(item);
    }
}
