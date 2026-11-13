package org.skysongdev.skysongEconomy.business.module;

import java.util.List;

/** Where it is saved which modules each business picked. A deleted business's rows go with it. */
public interface ModulePickRepository {
    /** @return every saved module pick */
    List<ModulePick> loadAll();

    /** Saves a new module pick. */
    void save(ModulePick pick);

    /** Forgets a module pick, named by its business and number. */
    void delete(String businessId, int order);
}
