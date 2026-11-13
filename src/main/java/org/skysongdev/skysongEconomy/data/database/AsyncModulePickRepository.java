package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.module.ModulePickRepository;

import java.util.List;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}, so picking a module never
 * waits for the database. Loading stays on the calling thread.
 */
public final class AsyncModulePickRepository implements ModulePickRepository {
    private final ModulePickRepository delegate;
    private final BackgroundWriter writer;

    public AsyncModulePickRepository(ModulePickRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<ModulePick> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(ModulePick pick) {
        writer.run("save the module " + pick.moduleId() + " of " + pick.businessId(), () -> delegate.save(pick));
    }

    @Override
    public void delete(String businessId, int order) {
        writer.run("delete module pick " + order + " of " + businessId, () -> delegate.delete(businessId, order));
    }
}
