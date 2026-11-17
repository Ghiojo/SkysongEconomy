package org.skysongdev.skysongEconomy.upkeep;

/**
 * What a click or drag in an NPC's upkeep chest would do to the chest, as far as the rules care.
 * The server glue sorts each inventory action into one of these.
 */
public enum ChestMove {
    /** Nothing goes in or out of the chest, such as a click in the player's own inventory. */
    NONE,
    /** Items only go into the chest. */
    PUT_IN,
    /** Items only come out of the chest. */
    TAKE_OUT,
    /** Items go in and others come out, such as swapping the cursor with a chest slot. */
    SWAP,
    /** A move the rules don't follow item by item, such as collecting to the cursor; refused unless the player may take out. */
    OTHER;

    /**
     * @param mayTakeOut   whether the player may take items out (the NPC's {@code inventory-edit} action)
     * @param itemAccepted whether every item going in pays an entry of the NPC's cost; ignored when nothing goes in
     * @return whether the move is allowed
     */
    public boolean allowed(boolean mayTakeOut, boolean itemAccepted) {
        return switch (this) {
            case NONE -> true;
            case PUT_IN -> itemAccepted;
            case TAKE_OUT, OTHER -> mayTakeOut;
            case SWAP -> mayTakeOut && itemAccepted;
        };
    }
}
