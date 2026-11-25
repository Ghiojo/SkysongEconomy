package org.skysongdev.skysongEconomy.api.business;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An NPC that fills one of a business's NPC slots.
 *
 * @param npc        its SkysongNPCs id
 * @param businessId the business it belongs to
 * @param slotSource what gives the slot it fills, such as {@code skill:martial} or {@code module:kennel}
 * @param createdAt  when the business created it
 */
public record BusinessNpcInfo(UUID npc, String businessId, String slotSource, Instant createdAt) {
    public BusinessNpcInfo {
        Objects.requireNonNull(npc, "npc");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(slotSource, "slotSource");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
