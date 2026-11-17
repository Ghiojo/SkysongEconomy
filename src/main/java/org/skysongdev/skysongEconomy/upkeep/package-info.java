/**
 * NPC upkeep: NPCs that need a supply of items every cycle.
 * <p>
 * An NPC has an upkeep when its SkysongNPCs template is listed in {@code npc-upkeep.yml}, or when
 * staff gave it one with {@code /se upkeep set}. Each cycle it eats its profile's cost from its
 * upkeep stock, all or nothing. A cycle it can't pay makes it {@link org.skysongdev.skysongEconomy.upkeep.UpkeepState#UNMAINTAINED};
 * an unmaintained NPC eats nothing until it is maintained again. Unmaintained is only a marker:
 * the NPC keeps working, and staff decide what it means.
 * <p>
 * The rules ({@link org.skysongdev.skysongEconomy.upkeep.UpkeepService}, {@link org.skysongdev.skysongEconomy.upkeep.UpkeepPayment})
 * have no server classes. They reach the NPCs through
 * {@link org.skysongdev.skysongEconomy.upkeep.UpkeepNpcs} and the stocked items through
 * {@link org.skysongdev.skysongEconomy.upkeep.UpkeepStock}, so they are unit tested with fakes. The
 * real item stacks ({@link org.skysongdev.skysongEconomy.upkeep.StockedItems}), the shared chests
 * ({@link org.skysongdev.skysongEconomy.upkeep.UpkeepChests}) and texts live next to them.
 */
package org.skysongdev.skysongEconomy.upkeep;
