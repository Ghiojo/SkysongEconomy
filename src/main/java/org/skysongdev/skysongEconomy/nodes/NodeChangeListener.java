package org.skysongdev.skysongEconomy.nodes;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;

/**
 * Told about every change {@link NodeService} makes, after it is made, so features such as the
 * floating name above the block can follow nodes without the service knowing about them.
 */
public interface NodeChangeListener {
    /**
     * A node was created or changed.
     *
     * @param previousPosition where its block was before the change, {@code null} if it had none
     *                         or was just created
     */
    default void changed(Node node, @Nullable BlockPosition previousPosition) {
    }

    /** A node was deleted. */
    default void deleted(Node node) {
    }

    /** Every node was (re)loaded from storage. */
    default void loaded(Iterable<Node> nodes) {
    }
}
