/**
 * Weighted loot tables: what a gathering node (or anything else that hands out random items)
 * gives for one harvest.
 * <p>
 * A {@link org.skysongdev.skysongEconomy.loot.LootGroup} is a list of weighted entries; each
 * roll adds all weights together, picks a number below the total and walks the list to see where
 * it lands, so an entry of weight 1 next to one of weight 4 is picked 1 time in 5. An entry is an
 * item with a min-max amount, another group to roll inside, or nothing.
 * A {@link org.skysongdev.skysongEconomy.loot.LootOutput} combines guaranteed items, items with
 * an independent chance, and rolls on groups.
 * <p>
 * This package has no server dependencies, so all of it is unit tested.
 */
package org.skysongdev.skysongEconomy.loot;
