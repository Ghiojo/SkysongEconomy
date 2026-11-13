package org.skysongdev.skysongEconomy.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.module.BusinessModuleEvents.RemovalCause;
import org.skysongdev.skysongEconomy.business.module.ModulePick;

/** A business no longer has a module it picked, and that pick is free again. */
public final class BusinessModuleRemoveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Business business;
    private final ModulePick pick;
    private final RemovalCause cause;

    public BusinessModuleRemoveEvent(Business business, ModulePick pick, RemovalCause cause) {
        this.business = business;
        this.pick = pick;
        this.cause = cause;
    }

    public Business getBusiness() {
        return business;
    }

    /** @return the pick the business had: the module's id and the skill it was picked for */
    public ModulePick getPick() {
        return pick;
    }

    /** @return why the business no longer has it */
    public RemovalCause getCause() {
        return cause;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
