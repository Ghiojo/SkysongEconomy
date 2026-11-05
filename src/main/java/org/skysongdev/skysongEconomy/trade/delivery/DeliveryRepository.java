package org.skysongdev.skysongEconomy.trade.delivery;

import java.util.List;

/** Where deliveries are saved until they are collected. */
public interface DeliveryRepository {
    List<Delivery> loadAll();

    void save(Delivery delivery);

    void delete(String id);
}
