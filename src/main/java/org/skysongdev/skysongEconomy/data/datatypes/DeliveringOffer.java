package org.skysongdev.skysongEconomy.data.datatypes;

import java.sql.Timestamp;

public class DeliveringOffer {
    private String offerName;
    private Timestamp deliveryDate;

    public DeliveringOffer(String offerName, Timestamp deliveryDate) {
        this.offerName = offerName;
        this.deliveryDate = deliveryDate;
    }

    public String getOfferName() {
        return offerName;
    }
    public Timestamp getDeliveryDate() {
        return deliveryDate;
    }
}
