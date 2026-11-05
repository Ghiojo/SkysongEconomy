package org.skysongdev.skysongEconomy.business;

import org.skysongdev.skysongEconomy.blocks.BlockGuard;
import org.skysongdev.skysongEconomy.blocks.GuardedBlock;

import java.util.Map;
import java.util.Optional;

/**
 * Keeps business blocks in place. Staff with {@link #BREAK_PERMISSION} may break one: the
 * business then keeps all its data without a block until it is moved.
 */
public final class BusinessBlockGuard implements BlockGuard {
    /** Lets staff break business blocks. */
    public static final String BREAK_PERMISSION = "skysongeconomy.business.break";

    private final BusinessService businesses;

    public BusinessBlockGuard(BusinessService businesses) {
        this.businesses = businesses;
    }

    @Override
    public Optional<GuardedBlock> at(BlockPosition position) {
        return businesses.at(position).map(business -> new GuardedBlock(business.displayName(), BREAK_PERMISSION,
                "business.block.protected", "business.block.unplaced",
                Map.of("business", business.displayName(), "id", business.id()), () -> businesses.unplace(business)));
    }
}
