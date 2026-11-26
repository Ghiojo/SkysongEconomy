package org.skysongdev.skysongEconomy.integration.skysongcrafting;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.skysongdev.skysongCrafting.api.WorkstationRemoveEvent;
import org.skysongdev.skysongEconomy.business.bench.BenchRegistry;

/** Unties a bench from its business when SkysongCrafting removes it: deleted, broken or replaced. */
public final class WorkstationRemovalListener implements Listener {
    private final BenchRegistry benches;

    public WorkstationRemovalListener(BenchRegistry benches) {
        this.benches = benches;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(WorkstationRemoveEvent event) {
        benches.benchRemoved(SkysongCraftingBenches.position(event.getWorkstation()));
    }
}
