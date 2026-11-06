package org.skysongdev.skysongEconomy.nodes.farm;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.items.StoredItems;

import java.util.Objects;

/**
 * What is planted in one farm node. The node's farm type and owner at planting time are kept,
 * so the primer goes back to whoever planted it as soon as either changes.
 *
 * @param nodeId     the farm node
 * @param resourceId the farm resource it grows
 * @param farmType   the node's farm type when it was planted
 * @param businessId the business that owned the node when it was planted
 * @param planter    the character who planted it
 * @param primer     the exact items put in, given back when it is taken out
 */
public record Planting(String nodeId, String resourceId, String farmType, String businessId, CharacterRef planter,
                       StoredItems primer) {
    public Planting {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(farmType, "farmType");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(planter, "planter");
        Objects.requireNonNull(primer, "primer");
    }
}
