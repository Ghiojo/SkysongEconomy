package org.skysongdev.skysongEconomy.business.module;

import java.util.List;

/**
 * One skill's module picks at a business.
 *
 * @param skill the skill
 * @param total how many module picks the skill gives the business now
 * @param picks the modules picked for it, oldest first
 */
public record ModulePickGroup(String skill, int total, List<ModulePick> picks) {
    public ModulePickGroup {
        picks = List.copyOf(picks);
    }

    /** @return how many picks are still free */
    public int free() {
        return Math.max(0, total - picks.size());
    }
}
