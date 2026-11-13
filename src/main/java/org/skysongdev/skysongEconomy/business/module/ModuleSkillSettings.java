package org.skysongdev.skysongEconomy.business.module;

import org.skysongdev.skysongEconomy.business.scale.Amount;

import java.util.List;

/**
 * What one skill gives in {@code businesses/modules.yml}.
 *
 * @param picks how many module picks the skill gives in total at a level and tier
 * @param pool  the modules those picks can take, by id, in file order
 */
public record ModuleSkillSettings(Amount picks, List<String> pool) {
    public ModuleSkillSettings {
        pool = List.copyOf(pool);
    }
}
