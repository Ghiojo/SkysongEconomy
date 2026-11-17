package org.skysongdev.skysongEconomy.upkeep;

import java.util.List;
import java.util.UUID;

/**
 * The items players put in NPCs for their upkeep. On the server they are real item stacks, kept
 * in the database; the rules only see what each stack is and how many it holds.
 */
public interface UpkeepStock {
    /** @return the NPC's stacks, in their order; empty when it has none */
    List<StockStack> view(UUID npc);

    /**
     * Takes items out of the NPC's stock.
     *
     * @param taken how many to take from each stack, by its place in {@link #view}, as
     *              {@link UpkeepPayment.Paid#taken()} gives them
     */
    void take(UUID npc, List<Integer> taken);
}
