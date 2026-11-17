package org.skysongdev.skysongEconomy.upkeep;

/** Whether an NPC's upkeep is being paid. */
public enum UpkeepState {
    /** Its cycles are paid; each one eats its cost. */
    MAINTAINED,
    /**
     * A cycle came and its cost was not there. Only a marker: the NPC works as before. Its
     * cycles eat nothing until it is maintained again.
     */
    UNMAINTAINED
}
