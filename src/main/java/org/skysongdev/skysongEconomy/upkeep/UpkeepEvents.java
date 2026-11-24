package org.skysongdev.skysongEconomy.upkeep;

import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.time.Instant;
import java.util.List;

/** Tells other plugins when an NPC becomes maintained or unmaintained, and when it pays a cycle. On the server these are Bukkit events. */
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

    /**
     * A cycle came and its cost was eaten from the NPC's stock. Cycles missed while the server was
     * down are each reported, in order.
     *
     * @param upkeep the NPC's upkeep after paying, with its next cycle
     * @param due    when the paid cycle was due
     * @param eaten  the items taken from the stock, each material once
     */
    default void cyclePaid(NpcUpkeep upkeep, Instant due, List<ItemAmount> eaten) {
    }
}
