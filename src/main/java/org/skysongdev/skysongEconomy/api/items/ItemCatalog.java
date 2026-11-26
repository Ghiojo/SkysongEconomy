package org.skysongdev.skysongEconomy.api.items;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Reads what SkysongEconomy knows about Skysong items: their base values from the price database
 * ({@code /se ledger}) and their good types ({@code /se good}, the item categories such as
 * {@code weapon} or {@code food}). Read-only. Registered with Bukkit's services manager while the
 * plugin is enabled:
 *
 * <pre>{@code
 * ItemCatalog items = Bukkit.getServicesManager().load(ItemCatalog.class);
 * Optional<BigDecimal> worth = items.baseValue("steel_sword");
 * boolean weapon = items.hasGoodType("steel_sword", "weapon");
 * }</pre>
 *
 * Call it on the main thread. Material ids and good types ignore case and surrounding spaces.
 * Everything it returns is an immutable copy taken when it was called.
 * <p>
 * Methods are only ever added, never removed or changed, within a major version.
 */
public interface ItemCatalog {
    /**
     * @return the item's base value, truncated to the decimals of {@code prices.yml} as players see
     *         it; empty when it has none or it can't be worked out (for example it divides by zero)
     */
    Optional<BigDecimal> baseValue(String materialId);

    /**
     * @return every item that has a base value, by lower-case material id in alphabetical order;
     *         items whose value can't be worked out are left out
     */
    SortedMap<String, BigDecimal> baseValues();

    /** @return how many decimals prices keep ({@code prices.yml}, 0 to 4) */
    int decimals();

    /** @return the item's good types, alphabetically; empty when it has none */
    Set<String> goodTypes(String materialId);

    /** @return whether the item has that good type */
    boolean hasGoodType(String materialId, String goodType);

    /** @return every good type at least one item has, alphabetically */
    SortedSet<String> allGoodTypes();

    /** @return the lower-case material ids of the items that have that good type, alphabetically */
    SortedSet<String> itemsOf(String goodType);

    /** @return the Skysong material id of the stack; empty for null, air or an item that isn't a Skysong item */
    Optional<String> materialId(@Nullable ItemStack item);
}
