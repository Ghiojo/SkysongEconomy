package org.skysongdev.skysongEconomy.api.business;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * An NPC's upkeep: what it eats every cycle and whether it is maintained.
 *
 * @param npc        its Citizens UUID
 * @param profile    the upkeep profile it follows; empty when staff took its upkeep away
 * @param setByHand  whether staff chose it with {@code /se upkeep set}, which wins over the template
 * @param maintained whether it is maintained; unmaintained is only a marker other plugins may act on
 * @param nextDue    when its next cycle comes; only counts while it is maintained
 * @param changed    when its state last changed, or it got this upkeep
 */
public record NpcUpkeepInfo(UUID npc, Optional<String> profile, boolean setByHand, boolean maintained, Instant nextDue,
                            Instant changed) {
    public NpcUpkeepInfo {
        Objects.requireNonNull(npc, "npc");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(nextDue, "nextDue");
        Objects.requireNonNull(changed, "changed");
    }
}
