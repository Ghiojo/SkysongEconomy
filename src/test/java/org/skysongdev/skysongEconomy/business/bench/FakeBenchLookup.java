package org.skysongdev.skysongEconomy.business.bench;

import org.skysongdev.skysongEconomy.business.BlockPosition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Benches tests place by hand, as SkysongCrafting would have them. */
public final class FakeBenchLookup implements BenchLookup {
    public final Map<BlockPosition, String> benches = new HashMap<>();
    public boolean installed = true;

    @Override
    public boolean installed() {
        return installed;
    }

    @Override
    public Optional<String> typeAt(BlockPosition block) {
        return installed ? Optional.ofNullable(benches.get(block)) : Optional.empty();
    }
}
