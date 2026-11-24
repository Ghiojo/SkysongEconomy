package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** A business no longer has a module it picked, and that pick is free again. Fired on the main thread. */
public final class BusinessModuleRemoveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final ModulePickInfo pick;
    private final ModuleRemovalCause cause;

    public BusinessModuleRemoveEvent(@NotNull BusinessInfo business, @NotNull ModulePickInfo pick,
                                     @NotNull ModuleRemovalCause cause) {
        this.business = Objects.requireNonNull(business, "business");
        this.pick = Objects.requireNonNull(pick, "pick");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** @return the business, as it was when this fired */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return the pick the business had: the module's id and the skill it was picked for */
    public @NotNull ModulePickInfo getPick() {
        return pick;
    }

    /** @return why the business no longer has it */
    public @NotNull ModuleRemovalCause getCause() {
        return cause;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
