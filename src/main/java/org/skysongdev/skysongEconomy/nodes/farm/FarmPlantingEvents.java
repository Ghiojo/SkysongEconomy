package org.skysongdev.skysongEconomy.nodes.farm;

import org.skysongdev.skysongEconomy.api.nodes.PlantingChange;

/**
 * Told when a primer is planted, taken out or given back, after it is saved, so other plugins can
 * hear about it. Called on the main thread.
 */
public interface FarmPlantingEvents {
    /** Tells nobody. */
    FarmPlantingEvents NONE = new FarmPlantingEvents() {
    };

    /**
     * @param change  what happened
     * @param planting the planting it happened to, with the primer's exact items
     */
    default void changed(PlantingChange change, Planting planting) {
    }
}
