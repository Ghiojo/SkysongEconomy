/**
 * What the business features need from SkysongNPCs, written as small interfaces.
 * <p>
 * A business NPC is an ordinary SkysongNPC that a business created and keeps giving things to.
 * SkysongNPCs is optional, and its API needs a running server, so the business rules never touch
 * it directly. They talk to three gateways instead:
 * <ul>
 *   <li>{@link org.skysongdev.skysongEconomy.business.npc.NpcGateway}: creating and removing NPCs
 *       and who owns them;</li>
 *   <li>{@link org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway}: what an NPC was given
 *       (stat points, proficiency picks, abilities, baked stats, fixed proficiencies);</li>
 *   <li>{@link org.skysongdev.skysongEconomy.business.npc.NpcAccessGateway}: who may do what with
 *       a business's NPCs.</li>
 * </ul>
 * {@code integration.skysongnpcs.SkysongNpcsAdapter} is the only implementation on a server; the
 * unit tests use in-memory fakes. Everything Economy gives an NPC carries a source text, so it
 * can be counted and taken back without touching what a template, staff or another plugin gave.
 * <p>
 * {@link org.skysongdev.skysongEconomy.business.npc.BusinessGroupRef} and
 * {@link org.skysongdev.skysongEconomy.business.npc.BusinessGroupDirectory} go the other way:
 * they let SkysongNPCs ask which characters are in a business's access groups.
 */
package org.skysongdev.skysongEconomy.business.npc;
