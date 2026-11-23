package org.skysongdev.skysongEconomy.api.piers;

/** What happened to a collected shipment. */
public enum ShipmentOutcome {
    /** Its items were handed over (possibly changed, or none, by a listener). */
    DELIVERED,
    /** It was lost at its pier: nothing was handed over and nothing refunded. */
    SEIZED
}
