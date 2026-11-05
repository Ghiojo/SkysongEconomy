package org.skysongdev.skysongEconomy.business;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.Objects;

/**
 * An immutable copy of a business's saved state, handed to storage so it can be written on
 * another thread while the live {@link Business} keeps changing.
 *
 * @param owner        the owning character, {@code null} when the server owns the business
 * @param position     the business block, {@code null} while the business has no block
 * @param shiftMessage the owner's shift message, {@code null} for the server default
 * @param workers      in hire order, oldest first
 * @param picks        the skill picks, oldest first; at most one per tier
 */
public record BusinessSnapshot(String id, String displayName, String typeId, int tier, @Nullable CharacterRef owner,
                               @Nullable BlockPosition position, @Nullable String shiftMessage,
                               List<Hire> workers, List<SkillPick> picks) {
    public BusinessSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(typeId, "typeId");
        workers = List.copyOf(workers);
        picks = List.copyOf(picks);
    }
}
