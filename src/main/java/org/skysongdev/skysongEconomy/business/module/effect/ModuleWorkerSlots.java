package org.skysongdev.skysongEconomy.business.module.effect;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.Hire;
import org.skysongdev.skysongEconomy.business.module.BusinessModuleEvents;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.scale.BusinessScale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code worker-slots} effect: picked modules let a business hire more workers than its
 * tier allows. When a module with the effect is removed, the newest hires the business no longer
 * has room for are removed too.
 */
public final class ModuleWorkerSlots implements BusinessModuleEvents {
    private final BusinessModules modules;
    private final BusinessService businesses;
    /** Hires removed with a module, by business id, until someone reports them. */
    private final Map<String, List<Hire>> lostWorkers = new HashMap<>();

    public ModuleWorkerSlots(BusinessModules modules, BusinessService businesses) {
        this.modules = modules;
        this.businesses = businesses;
    }

    /** @return how many more workers the business's modules let it hire right now */
    public int extra(Business business) {
        int extra = 0;
        for (ModulePick pick : modules.of(business)) {
            BusinessScale scale = BusinessScale.of(business, pick.skill());
            extra += modules.module(pick.moduleId()).stream()
                    .flatMap(module -> module.effects(ModuleEffect.WorkerSlots.class).stream())
                    .mapToInt(slots -> slots.amount().at(scale)).sum();
        }
        return extra;
    }

    /**
     * @return the workers the business lost with a module since this was last asked, newest
     *         first, so the command that caused it can say so
     */
    public List<Hire> takeLostWorkers(Business business) {
        List<Hire> lost = lostWorkers.remove(business.id());
        return lost == null ? List.of() : lost;
    }

    @Override
    public void picked(Business business, ModulePick pick) {
    }

    /** A removed module may leave the business with more workers than it has room for. */
    @Override
    public void removed(Business business, ModulePick pick, RemovalCause cause) {
        if (cause == RemovalCause.BUSINESS_DELETED) {
            lostWorkers.remove(business.id());
            return;
        }
        List<Hire> removed = businesses.trimWorkers(business);
        if (!removed.isEmpty()) {
            lostWorkers.computeIfAbsent(business.id(), id -> new ArrayList<>()).addAll(0, removed);
        }
    }
}
