package org.skysongdev.skysongEconomy.nodes;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;

import java.util.Objects;

/**
 * An immutable copy of a node's saved state, handed to storage so it can be written on another
 * thread while the live {@link Node} keeps changing.
 *
 * @param source      the random table's or farm type's id, depending on {@code kind}
 * @param displayName the name staff gave it, {@code null} to use its table's or type's name
 * @param position    the node block, {@code null} while the node has no block
 */
public record NodeSnapshot(String id, NodeKind kind, String source, int tier, NodeOwner owner,
                           @Nullable String displayName, @Nullable BlockPosition position) {
    public NodeSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(owner, "owner");
    }
}
