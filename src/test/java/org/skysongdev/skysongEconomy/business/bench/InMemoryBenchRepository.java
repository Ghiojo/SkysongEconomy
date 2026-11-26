package org.skysongdev.skysongEconomy.business.bench;

import org.skysongdev.skysongEconomy.business.BlockPosition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps bench links in a map, so tests can check what was written. */
public final class InMemoryBenchRepository implements BenchRepository {
    public final Map<BlockPosition, BusinessBench> saved = new LinkedHashMap<>();

    @Override
    public List<BusinessBench> loadAll() {
        return new ArrayList<>(saved.values());
    }

    @Override
    public void save(BusinessBench bench) {
        saved.put(bench.bench(), bench);
    }

    @Override
    public void delete(BlockPosition bench) {
        saved.remove(bench);
    }
}
