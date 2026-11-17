package org.skysongdev.skysongEconomy.upkeep;

/** Tells other plugins when an NPC becomes maintained or unmaintained. On the server these are Bukkit events. */
public interface UpkeepEvents {
    /** Nobody is told. */
    UpkeepEvents NONE = (upkeep, cause) -> {
    };

    /** Why an NPC's upkeep state changed. */
    enum Cause {
        /** A cycle came and its cost was not in the stock. */
        CYCLE,
        /** The {@code unmaintained} trait was taken off the NPC's item, such as by a SkysongCrafting recipe. */
        REPAIR,
        /** Staff marked it with {@code /se upkeep mark}. */
        STAFF
    }

    /** @param upkeep the NPC's upkeep after the change */
    void stateChanged(NpcUpkeep upkeep, Cause cause);
}
