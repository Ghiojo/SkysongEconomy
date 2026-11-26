package org.skysongdev.skysongEconomy.integration.skysongcrafting;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.skysongdev.skysongCrafting.api.WorkstationAccessEvent;
import org.skysongdev.skysongEconomy.business.bench.BenchAccess;

/** Lets a business's owner and workers open its private benches (see {@link BenchAccess}). */
public final class WorkstationAccessListener implements Listener {
    private final BenchAccess access;

    public WorkstationAccessListener(BenchAccess access) {
        this.access = access;
    }

    @EventHandler
    public void onAccess(WorkstationAccessEvent event) {
        if (!event.isAllowed()
                && access.mayUse(SkysongCraftingBenches.position(event.getWorkstation()), event.getPlayer().getUniqueId())) {
            event.setAllowed(true);
        }
    }
}
