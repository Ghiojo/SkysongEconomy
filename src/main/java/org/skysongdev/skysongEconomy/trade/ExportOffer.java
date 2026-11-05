package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.prices.PriceExpression;

import java.util.List;
import java.util.Objects;

/**
 * Something a ledger lets businesses sell: exactly {@code batch} items per sale, all or nothing.
 *
 * @param id          the key in the ledger file, unique within its ledger
 * @param displayName MiniMessage name of its button
 * @param icon        Minecraft material of its button
 * @param lore        MiniMessage lines under its name
 * @param target      which items count
 * @param batch       how many are sold per click, 1 or more
 * @param price       what one batch pays; {@code {base}} is the base value of the items sold
 */
public record ExportOffer(String id, String displayName, String icon, List<String> lore, Target target, int batch,
                          PriceExpression price) implements TradeOffer {
    public ExportOffer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(price, "price");
        lore = List.copyOf(lore);
        if (batch < 1) {
            throw new IllegalArgumentException("batch must be 1 or more: " + batch);
        }
    }

    /** Which items an export buys. */
    public sealed interface Target {
        /** One item: {@code item: steel_ingot}. */
        record Item(String materialId) implements Target {
            public Item {
                Objects.requireNonNull(materialId, "materialId");
            }
        }

        /** Any item of a good type (see {@code /se good}): {@code good-type: weapon}. */
        record GoodType(String goodType) implements Target {
            public GoodType {
                Objects.requireNonNull(goodType, "goodType");
            }
        }
    }
}
