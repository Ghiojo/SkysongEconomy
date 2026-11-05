package org.skysongdev.skysongEconomy.business;

import java.util.List;

/**
 * What a tier change took away. Going down a tier removes the newest hires and the newest skill
 * picks that the new tier has no room for; going up removes nothing.
 *
 * @param removedWorkers newest first
 * @param removedPicks   newest first
 */
public record TierChange(List<Hire> removedWorkers, List<SkillPick> removedPicks) {
    public TierChange {
        removedWorkers = List.copyOf(removedWorkers);
        removedPicks = List.copyOf(removedPicks);
    }
}
