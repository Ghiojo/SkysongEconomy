package org.skysongdev.skysongEconomy.blocks;

import org.skysongdev.skysongEconomy.business.BlockPosition;

import java.util.Optional;

/**
 * A feature whose blocks must stay in place, such as businesses and gathering nodes. The
 * protection listener asks every guard about a block before letting it break, and features ask
 * each other's guards so two of them never share a block.
 */
@FunctionalInterface
public interface BlockGuard {
    /** @return what holds that block, empty when this feature doesn't */
    Optional<GuardedBlock> at(BlockPosition position);
}
