package org.skysongdev.skysongEconomy.business.module;

import org.skysongdev.skysongEconomy.business.Business;

/** Tells other plugins about module picks. On the server these are Bukkit events. */
public interface BusinessModuleEvents {
    /** Nobody is told. */
    BusinessModuleEvents NONE = new BusinessModuleEvents() {
        @Override
        public void picked(Business business, ModulePick pick) {
        }

        @Override
        public void removed(Business business, ModulePick pick, RemovalCause cause) {
        }
    };

    /** Why a business no longer has a module. */
    enum RemovalCause {
        /** Staff freed the pick with {@code /se business clearmodule}. */
        CLEARED,
        /** The skill gives fewer module picks now: a lower tier, a cleared skill pick, or changed config. */
        PICK_LOST,
        /** The business was deleted. */
        BUSINESS_DELETED
    }

    /** A business picked a module. */
    void picked(Business business, ModulePick pick);

    /** A business no longer has a module it picked. */
    void removed(Business business, ModulePick pick, RemovalCause cause);
}
