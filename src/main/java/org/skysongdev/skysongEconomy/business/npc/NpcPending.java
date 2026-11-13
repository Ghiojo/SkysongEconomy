package org.skysongdev.skysongEconomy.business.npc;

/**
 * The choices an NPC still has to make, whoever gave them.
 *
 * @param points         stat points not assigned yet
 * @param picks          proficiency picks not spent yet
 * @param abilityChoices abilities not chosen yet
 */
public record NpcPending(int points, int picks, int abilityChoices) {
    /** An NPC with nothing left to choose. */
    public static final NpcPending NONE = new NpcPending(0, 0, 0);

    public boolean any() {
        return points > 0 || picks > 0 || abilityChoices > 0;
    }
}
