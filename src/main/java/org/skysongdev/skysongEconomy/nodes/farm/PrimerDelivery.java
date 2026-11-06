package org.skysongdev.skysongEconomy.nodes.farm;

import org.skysongdev.skysongEconomy.items.StoredItems;

import java.util.UUID;

/** Hands stored primers to a player; on the server it puts them in their inventory. */
@FunctionalInterface
public interface PrimerDelivery {
    /**
     * @return whether the player got the items; false when they are offline, so the items are
     *         kept for later
     */
    boolean tryGive(UUID player, StoredItems items);
}
