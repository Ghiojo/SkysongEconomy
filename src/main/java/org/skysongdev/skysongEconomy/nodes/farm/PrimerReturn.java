package org.skysongdev.skysongEconomy.nodes.farm;

import org.skysongdev.skysongEconomy.items.StoredItems;

import java.util.Objects;
import java.util.UUID;

/**
 * A primer waiting to go back to the player who planted it, because they were offline when its
 * node was removed, changed owner or changed farm type.
 *
 * @param id     tells waiting returns apart
 * @param player who gets it, the next time they are online
 * @param items  the exact items they planted
 */
public record PrimerReturn(UUID id, UUID player, StoredItems items) {
    public PrimerReturn {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(items, "items");
    }
}
