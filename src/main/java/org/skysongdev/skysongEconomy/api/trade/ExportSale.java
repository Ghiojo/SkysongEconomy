package org.skysongdev.skysongEconomy.api.trade;

import org.skysongdev.skysongEconomy.api.items.ItemCount;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One batch of an export sold, or about to be sold, at a business. Exports go through no pier.
 *
 * @param id         unique, generated for each sale
 * @param businessId the business it is sold at
 * @param ledgerId   the ledger it is sold through
 * @param exportId   which export of that ledger
 * @param seller     the character selling; {@code seller.player()} is the account that is paid
 * @param items      what the batch takes from the seller, each material once
 * @param price      what the seller is paid, with every modifier that applied
 * @param soldAt     when it is sold
 * @param tags       the ledger's and the export's tags, such as {@code contraband}
 */
public record ExportSale(String id, String businessId, String ledgerId, String exportId, CharacterRef seller,
                         List<ItemCount> items, TradePrice price, Instant soldAt, Set<String> tags) {
    public ExportSale {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(ledgerId, "ledgerId");
        Objects.requireNonNull(exportId, "exportId");
        Objects.requireNonNull(seller, "seller");
        items = List.copyOf(items);
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(soldAt, "soldAt");
        tags = Set.copyOf(tags);
    }

    /** @return whether it carries that tag */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
}
