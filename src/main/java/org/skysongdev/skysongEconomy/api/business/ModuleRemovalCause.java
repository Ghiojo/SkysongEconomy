package org.skysongdev.skysongEconomy.api.business;

/** Why a business no longer has a module it picked. */
public enum ModuleRemovalCause {
    /** Its owner or staff cleared the pick. */
    CLEARED,
    /** The business lost the skill pick the module was picked for. */
    PICK_LOST,
    /** The business was deleted. */
    BUSINESS_DELETED
}
