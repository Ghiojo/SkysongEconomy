package org.skysongdev.skysongEconomy.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.module.ModulePick;

/** A business picked a module for one of its skills. The pick is saved already. */
public final class BusinessModulePickEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Business business;
    private final ModulePick pick;

    public BusinessModulePickEvent(Business business, ModulePick pick) {
        this.business = business;
        this.pick = pick;
    }

    public Business getBusiness() {
        return business;
    }

    /** @return the pick: the module's id, the skill it was picked for and when */
    public ModulePick getPick() {
        return pick;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
