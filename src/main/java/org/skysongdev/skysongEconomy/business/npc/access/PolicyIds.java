package org.skysongdev.skysongEconomy.business.npc.access;

import org.skysongdev.skysongEconomy.business.npc.NpcAccessGateway;

/**
 * Names the SkysongNPCs access policy of each business: {@code economy.<business id>}. A policy
 * id may be {@value NpcAccessGateway#MAX_POLICY_ID_LENGTH} characters and a business id almost
 * as long, so an id that would not fit is cut and ends in a number made from the whole id, which
 * keeps two long ids apart.
 */
public final class PolicyIds {
    private static final String PREFIX = "economy.";

    private PolicyIds() {
    }

    /** @return the id of the policy that business's NPCs follow; always the same for the same business id */
    public static String of(String businessId) {
        String plain = PREFIX + businessId;
        if (plain.length() <= NpcAccessGateway.MAX_POLICY_ID_LENGTH) {
            return plain;
        }
        String suffix = "." + Integer.toHexString(businessId.hashCode());
        return plain.substring(0, NpcAccessGateway.MAX_POLICY_ID_LENGTH - suffix.length()) + suffix;
    }
}
