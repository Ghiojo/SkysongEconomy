package org.skysongdev.skysongEconomy.api.nodes;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Instant;
import java.util.Objects;

/**
 * A primer planted in, or taken out of, a farm node.
 *
 * @param kind         what happened
 * @param nodeId       the farm node
 * @param resourceId   the farm resource the primer grows, such as {@code wheat}
 * @param farmType     the node's farm type when it was planted, such as {@code crop}
 * @param businessId   the business that owned the node when it was planted
 * @param planter      who planted it
 * @param actor        who planted or took it out; the planter when it was given back
 * @param downtimePaid the downtime spent taking it out; 0 otherwise
 * @param at           when it happened
 */
public record PlantingChange(Kind kind, String nodeId, String resourceId, String farmType, String businessId,
                             CharacterRef planter, CharacterRef actor, int downtimePaid, Instant at) {
    public PlantingChange {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(farmType, "farmType");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(planter, "planter");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(at, "at");
    }

    /** What happened to the planting. New kinds may be added. */
    public enum Kind {
        /** A primer was put in an empty farm node, for free. */
        PLANTED,
        /** Someone who tends the farm took the primer out, paying the removal downtime. */
        TAKEN_OUT,
        /**
         * The primer went back to its planter (at once, or when they next join) because its node was
         * removed or changed owner or farm type, which includes its business being deleted.
         */
        RETURNED
    }
}
