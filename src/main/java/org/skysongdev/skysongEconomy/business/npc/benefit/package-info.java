/**
 * What a business gives its NPCs, and keeping every NPC in line with it.
 * <p>
 * A {@link org.skysongdev.skysongEconomy.business.npc.benefit.Benefit} is the block config
 * writes; a {@link org.skysongdev.skysongEconomy.business.npc.benefit.Progression} is a benefit
 * that grows with the business. At a business's level and tier they come down to an
 * {@link org.skysongdev.skysongEconomy.business.npc.benefit.Entitlement}: plain numbers and ids.
 * {@link org.skysongdev.skysongEconomy.business.npc.benefit.NpcReconciler} compares an
 * entitlement with what an NPC was already given and applies only the difference. Nothing here
 * touches server classes.
 */
package org.skysongdev.skysongEconomy.business.npc.benefit;
