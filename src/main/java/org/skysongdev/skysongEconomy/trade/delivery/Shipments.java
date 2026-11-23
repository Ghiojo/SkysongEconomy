package org.skysongdev.skysongEconomy.trade.delivery;

import org.skysongdev.skysongEconomy.api.piers.Shipment;
import org.skysongdev.skysongEconomy.api.piers.ShipmentItem;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;

/** Turns deliveries into the {@link Shipment}s other plugins see. */
public final class Shipments {
    private Shipments() {
    }

    public static Shipment of(Delivery delivery) {
        return new Shipment(delivery.id(), delivery.businessId(), delivery.ledgerId(), delivery.importId(),
                delivery.deliveryTimeId(), delivery.pierId(), items(delivery.items()), delivery.orderedBy(),
                delivery.price(), delivery.orderedAt(), delivery.arrivesAt(), delivery.tags());
    }

    public static List<ShipmentItem> items(List<ItemAmount> items) {
        return items.stream().map(item -> new ShipmentItem(item.materialId(), item.amount())).toList();
    }
}
