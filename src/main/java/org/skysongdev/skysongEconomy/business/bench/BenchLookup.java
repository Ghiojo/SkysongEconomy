package org.skysongdev.skysongEconomy.business.bench;

import org.skysongdev.skysongEconomy.business.BlockPosition;

import java.util.Optional;

/** Finds SkysongCrafting benches, so only real benches can be tied to a business. */
public interface BenchLookup {
    /** Used when SkysongCrafting isn't installed: there are no benches to find. */
    BenchLookup NONE = new BenchLookup() {
        @Override
        public boolean installed() {
            return false;
        }

        @Override
        public Optional<String> typeAt(BlockPosition block) {
            return Optional.empty();
        }
    };

    /** @return whether SkysongCrafting is installed, so benches can be looked up at all */
    boolean installed();

    /** @return the type id of the bench on that block; empty when the block isn't a bench */
    Optional<String> typeAt(BlockPosition block);
}
