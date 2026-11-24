package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** A business picked a module for one of its skills. Fired on the main thread; the pick is saved already. */
public final class BusinessModulePickEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final ModulePickInfo pick;

    public BusinessModulePickEvent(@NotNull BusinessInfo business, @NotNull ModulePickInfo pick) {
        this.business = Objects.requireNonNull(business, "business");
        this.pick = Objects.requireNonNull(pick, "pick");
    }

    /** @return the business, as it was when this fired */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return the pick: the module's id, the skill it was picked for and when */
    public @NotNull ModulePickInfo getPick() {
        return pick;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
