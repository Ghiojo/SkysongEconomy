package org.skysongdev.skysongEconomy.business.npc;

import java.util.Set;

/**
 * Stat points one source gave an NPC in one grant.
 *
 * @param amount   how many points the grant holds
 * @param spent    how many of them were assigned to a stat already
 * @param eligible the stats they may go on; empty for the NPC's default eligible stats
 */
public record PointsGiven(int amount, int spent, Set<String> eligible) {
    public PointsGiven {
        eligible = Set.copyOf(eligible);
    }

    public int unspent() {
        return amount - spent;
    }
}
