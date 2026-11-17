package org.skysongdev.skysongEconomy.upkeep;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One NPC's upkeep, as saved.
 *
 * @param npc       the NPC's Citizens UUID
 * @param profile   the profile it follows, or {@code null} when staff took its upkeep away by
 *                  hand, which keeps its template from giving it one again
 * @param setByHand whether staff chose this with {@code /se upkeep set}, which wins over the template
 * @param state     whether it is maintained
 * @param nextDue   when its next cycle comes; only counts while it is maintained
 * @param changed   when its state last changed, or it got this upkeep
 */
public record NpcUpkeep(UUID npc, @Nullable String profile, boolean setByHand, UpkeepState state, Instant nextDue,
                        Instant changed) {
    public NpcUpkeep {
        Objects.requireNonNull(npc, "npc");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(nextDue, "nextDue");
        Objects.requireNonNull(changed, "changed");
        if (profile == null && !setByHand) {
            throw new IllegalArgumentException("Only staff can leave an NPC without upkeep on purpose");
        }
    }

    /** @return the profile it follows; empty when staff took its upkeep away */
    public Optional<String> profileId() {
        return Optional.ofNullable(profile);
    }

    /** @return whether it has an upkeep at all */
    public boolean hasUpkeep() {
        return profile != null;
    }

    NpcUpkeep withState(UpkeepState newState, Instant newNextDue, Instant now) {
        return new NpcUpkeep(npc, profile, setByHand, newState, newNextDue, now);
    }

    NpcUpkeep withNextDue(Instant newNextDue) {
        return new NpcUpkeep(npc, profile, setByHand, state, newNextDue, changed);
    }
}
