package org.skysongdev.skysongEconomy.trade.delivery;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An ordered import, filling one delivery slot of its business until it is collected. Its items
 * are rolled when it is ordered, so changing or removing the ledger doesn't change it.
 *
 * @param id             unique, generated
 * @param ledgerId       where it was ordered from
 * @param importId       which import of that ledger
 * @param deliveryTimeId which delivery time was chosen
 * @param items          what it brings
 * @param orderedBy      the character who ordered and paid
 * @param price          what they paid
 * @param arrivesAt      when it can be collected; real time, so it also passes while offline
 * @param pierId         the receiving pier picked when it was ordered; empty when there were no piers
 * @param tags           its ledger's and import's tags when it was ordered
 */
public record Delivery(String id, String businessId, String ledgerId, String importId, String deliveryTimeId,
                       List<ItemAmount> items, CharacterRef orderedBy, BigDecimal price, Instant orderedAt,
                       Instant arrivesAt, Optional<String> pierId, Set<String> tags) {
    public Delivery {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(ledgerId, "ledgerId");
        Objects.requireNonNull(importId, "importId");
        Objects.requireNonNull(deliveryTimeId, "deliveryTimeId");
        items = List.copyOf(items);
        Objects.requireNonNull(orderedBy, "orderedBy");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(orderedAt, "orderedAt");
        Objects.requireNonNull(arrivesAt, "arrivesAt");
        Objects.requireNonNull(pierId, "pierId");
        tags = Set.copyOf(tags);
    }

    /** @return whether it has arrived */
    public boolean isReady(Instant now) {
        return !now.isBefore(arrivesAt);
    }
}
