package org.skysongdev.skysongEconomy.api.business;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.Objects;
import java.util.Optional;

/**
 * One change to a business, after it was saved.
 *
 * @param kind      what changed
 * @param before    the business before; empty when it was just created
 * @param after     the business after; empty when it was deleted
 * @param character the worker hired or fired, when {@code kind} is about workers
 * @param skill     the skill picked or cleared, when {@code kind} is about skill picks
 */
public record BusinessChange(Kind kind, Optional<BusinessInfo> before, Optional<BusinessInfo> after,
                             Optional<CharacterRef> character, Optional<String> skill) {
    /** What changed about a business. */
    public enum Kind {
        /** It was created. */
        CREATED,
        /** It was deleted. */
        DELETED,
        /** Its display name changed. */
        RENAMED,
        /** Its block moved somewhere else. */
        MOVED,
        /** It lost its block and has none now. */
        UNPLACED,
        /** Its tier changed. Workers and skill picks it lost with it come as their own changes right after. */
        TIER_CHANGED,
        /** A character became its owner. */
        OWNER_CHANGED,
        /** The server became its owner. */
        GIVEN_TO_SERVER,
        /** It picked a skill. */
        SKILL_PICKED,
        /** It lost a skill pick, cleared or taken away by a lower tier. */
        SKILL_CLEARED,
        /** It hired a worker. */
        WORKER_HIRED,
        /** It lost a worker, fired or let go because it has fewer worker slots. */
        WORKER_FIRED
    }

    public BusinessChange {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(skill, "skill");
    }

    /** @return the business's id, before or after */
    public String businessId() {
        return after.or(() -> before).map(BusinessInfo::id).orElseThrow();
    }
}
