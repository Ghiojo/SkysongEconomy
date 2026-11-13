package org.skysongdev.skysongEconomy.business.npc;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * What came of asking for a new NPC.
 *
 * @param outcome whether it was created, or why not
 * @param id      the new NPC's id, {@code null} unless it was created
 */
public record NpcCreation(Outcome outcome, @Nullable UUID id) {
    /** Why an NPC was or was not created. */
    public enum Outcome {
        /** The NPC exists and is waiting for its first setup. */
        CREATED,
        /** No free spot nearby that the placer may use. */
        NO_SPOT,
        /** The player to stand in front of is offline, or the block's world is not loaded. */
        NOWHERE,
        /** SkysongNPCs has no template with that name. */
        UNKNOWN_TEMPLATE,
        /** SkysongNPCs or another plugin refused to spawn it. */
        REFUSED
    }

    public NpcCreation {
        Objects.requireNonNull(outcome, "outcome");
        if ((outcome == Outcome.CREATED) != (id != null)) {
            throw new IllegalArgumentException("Only a created NPC has an id: " + outcome + ", " + id);
        }
    }

    public static NpcCreation created(UUID id) {
        return new NpcCreation(Outcome.CREATED, Objects.requireNonNull(id, "id"));
    }

    /** @param outcome anything but {@link Outcome#CREATED} */
    public static NpcCreation failed(Outcome outcome) {
        return new NpcCreation(outcome, null);
    }

    public boolean isCreated() {
        return outcome == Outcome.CREATED;
    }

    /** @return the new NPC's id, empty when none was created */
    public Optional<UUID> npc() {
        return Optional.ofNullable(id);
    }
}
