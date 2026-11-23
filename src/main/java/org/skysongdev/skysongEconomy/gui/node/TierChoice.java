package org.skysongdev.skysongEconomy.gui.node;

import org.skysongdev.skysongEconomy.loot.LootOutput;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;
import org.skysongdev.skysongEconomy.nodes.modifiers.ActiveModifiers;

import java.util.Objects;
import java.util.Optional;

/**
 * One tier a node menu offers, as its button shows it.
 *
 * @param tier      the tier number
 * @param cost      what one harvest costs; empty when the tier isn't offered on this node, such as
 *                  a business-only tier on a public node
 * @param level     the skill level it needs
 * @param skill     the skill the level is checked in
 * @param output    what one harvest gives, before any change
 * @param modifiers the calendar modifiers on today for it
 */
record TierChoice(int tier, Optional<HarvestCost> cost, String level, String skill, LootOutput output,
                  ActiveModifiers modifiers) {
    TierChoice {
        Objects.requireNonNull(cost, "cost");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(modifiers, "modifiers");
    }
}
