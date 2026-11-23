package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.prices.PriceExpression;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Something a ledger lets businesses order.
 *
 * @param id          the key in the ledger file, unique within its ledger
 * @param displayName MiniMessage name of its button
 * @param icon        Minecraft material of its button, e.g. {@code IRON_SWORD}
 * @param lore        MiniMessage lines under its name
 * @param contents    what each delivery gives
 * @param prices      the price per delivery time id, in delivery time order; {@code {base}} is
 *                    the bundle's base value
 * @param tags        lower-case labels copied onto every shipment of it, with its ledger's, such as
 *                    {@code contraband}; other plugins read them
 */
public record ImportOffer(String id, String displayName, String icon, List<String> lore, Bundle contents,
                          Map<String, PriceExpression> prices, Set<String> tags) implements TradeOffer {
    public ImportOffer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(contents, "contents");
        lore = List.copyOf(lore);
        prices = Collections.unmodifiableMap(new LinkedHashMap<>(prices));
        tags = Set.copyOf(tags);
        if (prices.isEmpty()) {
            throw new IllegalArgumentException("An import needs a price for at least one delivery time");
        }
    }

    /** An import without tags. */
    public ImportOffer(String id, String displayName, String icon, List<String> lore, Bundle contents,
                       Map<String, PriceExpression> prices) {
        this(id, displayName, icon, lore, contents, prices, Set.of());
    }

    /** @return the price for that delivery time, empty when this import doesn't offer it */
    public Optional<PriceExpression> price(String deliveryTimeId) {
        return Optional.ofNullable(prices.get(deliveryTimeId));
    }
}
