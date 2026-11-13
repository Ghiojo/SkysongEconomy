package org.skysongdev.skysongEconomy.business.npc;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One NPC a business has: which SkysongNPC it is and which slot it fills. Everything else about
 * the NPC (name, sheet, inventory, where it stands) is SkysongNPCs' to keep.
 *
 * @param npc        the SkysongNPC's id
 * @param businessId the business it belongs to
 * @param source     what gave the slot it fills
 * @param createdAt  when the business created it; the newest NPCs go first when slots are lost
 */
public record BusinessNpc(UUID npc, String businessId, SlotSource source, Instant createdAt) {
    public BusinessNpc {
        Objects.requireNonNull(npc, "npc");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
