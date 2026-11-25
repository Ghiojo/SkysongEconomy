package org.skysongdev.skysongEconomy.api.nodes;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A primer was planted in a farm node, taken out, or given back to its planter. Fired on the main
 * thread, after the change is saved. Primers given back when the plugin starts (their node went
 * while it was off) fire too.
 */
public final class FarmPlantingChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final PlantingChange change;
    private final List<ItemStack> primer;

    public FarmPlantingChangeEvent(@NotNull PlantingChange change, @NotNull List<ItemStack> primer) {
        this.change = Objects.requireNonNull(change, "change");
        this.primer = List.copyOf(Objects.requireNonNull(primer, "primer"));
    }

    /** @return what happened */
    public @NotNull PlantingChange getChange() {
        return change;
    }

    /** @return copies of the primer's exact items; changing them changes nothing */
    public @NotNull List<ItemStack> getPrimer() {
        return primer;
    }

    /** @return what happened: planted, taken out or given back */
    public @NotNull PlantingChange.Kind getKind() {
        return change.kind();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
