package org.skysongdev.skysongEconomy.api.business;

/** Why an NPC became maintained or unmaintained. */
public enum UpkeepChangeCause {
    /** A cycle came and its cost was not in the stock. */
    CYCLE,
    /** The {@code unmaintained} trait was taken off the NPC's item, such as by a SkysongCrafting recipe. */
    REPAIR,
    /** Staff marked it with {@code /se upkeep mark}. */
    STAFF
}
