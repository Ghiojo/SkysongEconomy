package org.skysongdev.skysongEconomy.nodes.harvest;

/** Tells other plugins about harvests; on the server it fires {@code NodeHarvestEvent}. */
@FunctionalInterface
public interface NodeHarvestEvents {
    /** Tells nobody, for tests. */
    NodeHarvestEvents NONE = harvest -> {
    };

    /** A harvest was paid for and rolled; its items are handed over right after. */
    void harvested(Harvest harvest);
}
