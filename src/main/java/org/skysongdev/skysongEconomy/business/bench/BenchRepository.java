package org.skysongdev.skysongEconomy.business.bench;

import org.skysongdev.skysongEconomy.business.BlockPosition;

import java.util.List;

/** Where it is saved which benches belong to which business. A deleted business's rows go with it. */
public interface BenchRepository {
    /** @return every saved bench link */
    List<BusinessBench> loadAll();

    /** Saves a link, replacing the bench's earlier one when it moved to another business. */
    void save(BusinessBench bench);

    /** Forgets the link of the bench on that block. */
    void delete(BlockPosition bench);
}
