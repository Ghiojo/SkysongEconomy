package org.skysongdev.skysongEconomy.nodes.farm;

import java.util.List;
import java.util.UUID;

/** Where primers waiting for their offline planters are saved. */
public interface PrimerReturnRepository {
    /** @return every waiting return */
    List<PrimerReturn> loadAll();

    void save(PrimerReturn waiting);

    /** Deletes the return with that id, once it was handed over. */
    void delete(UUID id);
}
