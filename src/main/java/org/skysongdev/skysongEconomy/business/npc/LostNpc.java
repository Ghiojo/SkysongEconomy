package org.skysongdev.skysongEconomy.business.npc;

/**
 * A business NPC that was just removed, with the name it had, since the NPC itself can no
 * longer be asked.
 *
 * @param npc  the removed business NPC
 * @param name its name without formatting, or its id when it had none
 */
public record LostNpc(BusinessNpc npc, String name) {
}
