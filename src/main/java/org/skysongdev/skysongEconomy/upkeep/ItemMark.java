package org.skysongdev.skysongEconomy.upkeep;

/**
 * What to do with an NPC's item so it shows the NPC's upkeep state. An unmaintained NPC's item
 * carries the {@code unmaintained} trait together with a hidden {@code upkeep_marked} tag. The
 * tag is what tells "never marked" from "the trait was taken off": SkysongItems fires no event
 * when a trait is removed, so an item that still has the tag but lost the trait was repaired,
 * such as by a SkysongCrafting recipe.
 */
public enum ItemMark {
    /** The item already shows the state. */
    NONE,
    /** Add the trait and the tag: the NPC is unmaintained and its item doesn't say so yet. */
    MARK,
    /** Take the trait and the tag off: the NPC is maintained again, such as marked by staff. */
    UNMARK,
    /** The trait was taken off while the NPC was unmaintained: repair it, then take the tag off. */
    REPAIR;

    /**
     * @param state    the NPC's state, or {@code null} when it has no upkeep
     * @param hasTrait whether the item has the {@code unmaintained} trait
     * @param marked   whether the item has the {@code upkeep_marked} tag
     * @return what to do with the item
     */
    public static ItemMark decide(UpkeepState state, boolean hasTrait, boolean marked) {
        if (state != UpkeepState.UNMAINTAINED) {
            return hasTrait || marked ? UNMARK : NONE;
        }
        if (hasTrait) {
            return marked ? NONE : MARK;
        }
        return marked ? REPAIR : MARK;
    }
}
