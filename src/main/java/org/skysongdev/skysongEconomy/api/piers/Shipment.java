package org.skysongdev.skysongEconomy.api.piers;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An ordered import, on its way or waiting to be collected, as other plugins see it.
 *
 * @param id             unique
 * @param businessId     the business that ordered it
 * @param ledgerId       the ledger it was ordered from
 * @param importId       which import of that ledger
 * @param deliveryTimeId which delivery time was chosen
 * @param pierId         the pier it is sent to; empty for imports ordered while there were no piers
 * @param items          what it brings, rolled when it was ordered
 * @param orderedBy      the character who ordered and paid; {@code orderedBy.player()} is the account
 * @param price          what they paid
 * @param orderedAt      when it was ordered
 * @param arrivesAt      when it can be collected
 * @param tags           its ledger's and import's tags when it was ordered, such as {@code contraband}
 */
public record Shipment(String id, String businessId, String ledgerId, String importId, String deliveryTimeId,
                       Optional<String> pierId, List<ShipmentItem> items, CharacterRef orderedBy, BigDecimal price,
                       Instant orderedAt, Instant arrivesAt, Set<String> tags) {
    public Shipment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(ledgerId, "ledgerId");
        Objects.requireNonNull(importId, "importId");
        Objects.requireNonNull(deliveryTimeId, "deliveryTimeId");
        Objects.requireNonNull(pierId, "pierId");
        items = List.copyOf(items);
        Objects.requireNonNull(orderedBy, "orderedBy");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(orderedAt, "orderedAt");
        Objects.requireNonNull(arrivesAt, "arrivesAt");
        tags = Set.copyOf(tags);
    }

    /** @return whether it carries that tag */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
}
