package org.skysongdev.skysongEconomy.integration.skysongcrafting;

import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongCrafting.api.WorkstationInfo;
import org.skysongdev.skysongCrafting.api.Workstations;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.bench.BenchLookup;

import java.util.Optional;

/**
 * The only class that reads SkysongCrafting's {@link Workstations} service. It finds benches by
 * block, so nothing else depends on SkysongCrafting. Use it on the main thread.
 */
public final class SkysongCraftingBenches implements BenchLookup {
    private final Workstations workstations;

    private SkysongCraftingBenches(Workstations workstations) {
        this.workstations = workstations;
    }

    /**
     * @param plugin SkysongEconomy, to reach the services manager
     * @throws IllegalStateException when SkysongCrafting registered no {@link Workstations} service
     *                               (a version older than 1.0.0)
     */
    public static SkysongCraftingBenches connect(Plugin plugin) {
        Workstations workstations = plugin.getServer().getServicesManager().load(Workstations.class);
        if (workstations == null) {
            throw new IllegalStateException("SkysongCrafting has no workstation API: update it to 1.0.0 or newer");
        }
        return new SkysongCraftingBenches(workstations);
    }

    @Override
    public boolean installed() {
        return true;
    }

    @Override
    public Optional<String> typeAt(BlockPosition block) {
        return workstations.at(block.world(), block.x(), block.y(), block.z()).map(WorkstationInfo::typeId);
    }

    /** @return the plugin's block position for a workstation */
    static BlockPosition position(WorkstationInfo workstation) {
        return new BlockPosition(workstation.world(), workstation.x(), workstation.y(), workstation.z());
    }
}
