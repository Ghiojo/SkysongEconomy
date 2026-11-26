package org.skysongdev.skysongEconomy.events;

import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.benches.BenchAssignEvent;
import org.skysongdev.skysongEconomy.api.benches.BenchUnassignEvent;
import org.skysongdev.skysongEconomy.business.bench.BenchEvents;
import org.skysongdev.skysongEconomy.business.bench.BenchService;
import org.skysongdev.skysongEconomy.business.bench.BusinessBench;
import org.skysongdev.skysongEconomy.business.bench.UnassignCause;

import java.util.Optional;

/** Fires {@link BenchAssignEvent} and {@link BenchUnassignEvent}. */
public final class BukkitBenchEvents implements BenchEvents {
    private final PluginManager plugins;
    private final BenchService benches;

    public BukkitBenchEvents(PluginManager plugins, BenchService benches) {
        this.plugins = plugins;
        this.benches = benches;
    }

    @Override
    public void assigned(BusinessBench link, Optional<BusinessBench> previous) {
        plugins.callEvent(new BenchAssignEvent(benches.link(link), previous.map(benches::link)));
    }

    @Override
    public void unassigned(BusinessBench link, UnassignCause cause, Optional<String> by) {
        plugins.callEvent(new BenchUnassignEvent(benches.link(link), BenchService.cause(cause), by));
    }
}
