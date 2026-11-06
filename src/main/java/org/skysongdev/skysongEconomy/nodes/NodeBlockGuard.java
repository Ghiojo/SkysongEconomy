package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.blocks.BlockGuard;
import org.skysongdev.skysongEconomy.blocks.GuardedBlock;
import org.skysongdev.skysongEconomy.business.BlockPosition;

import java.util.Map;
import java.util.Optional;

/**
 * Keeps node blocks in place. Staff with {@link #BREAK_PERMISSION} may break one: the node then
 * keeps all its data without a block until it is moved.
 */
public final class NodeBlockGuard implements BlockGuard {
    /** Lets staff break node blocks; the same permission as every /se node command. */
    public static final String BREAK_PERMISSION = "skysongeconomy.nodes";

    private final NodeService nodes;

    public NodeBlockGuard(NodeService nodes) {
        this.nodes = nodes;
    }

    @Override
    public Optional<GuardedBlock> at(BlockPosition position) {
        return nodes.at(position).map(node -> new GuardedBlock(nodes.name(node), BREAK_PERMISSION,
                "node.block.protected", "node.block.unplaced",
                Map.of("node", nodes.name(node), "id", node.id()), () -> nodes.unplace(node)));
    }
}
