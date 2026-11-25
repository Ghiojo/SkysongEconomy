package org.skysongdev.skysongEconomy.api.business;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A business as other plugins see it, at the moment it was read. It is a copy: it doesn't change
 * when the business does.
 *
 * @param id          unique, lower case, such as {@code rose_smithy}
 * @param typeId      its business type, such as {@code crafting}
 * @param displayName the name players see
 * @param tier        its tier, from 1
 * @param owner       the character who owns it; empty when the server owns it
 * @param workers     the characters hired there, longest hired first
 */
public record BusinessInfo(String id, String typeId, String displayName, int tier, Optional<CharacterRef> owner,
                           List<CharacterRef> workers) {
    public BusinessInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(owner, "owner");
        workers = List.copyOf(workers);
    }

    /** @return whether the server owns it, so nobody is paid as its owner */
    public boolean serverOwned() {
        return owner.isEmpty();
    }
}
