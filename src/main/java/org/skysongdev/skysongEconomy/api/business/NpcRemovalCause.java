package org.skysongdev.skysongEconomy.api.business;

/** Why a business no longer has one of its NPCs. */
public enum NpcRemovalCause {
    /** Its owner or staff dismissed it. */
    DISMISSED,
    /** The business lost the slot it filled, such as by losing a module. */
    SLOT_LOST,
    /** The business was deleted. */
    BUSINESS_DELETED,
    /** It was removed in SkysongNPCs or Citizens, outside SkysongEconomy. */
    REMOVED_ELSEWHERE
}
