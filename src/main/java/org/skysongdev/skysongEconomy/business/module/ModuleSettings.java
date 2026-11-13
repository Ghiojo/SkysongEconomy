package org.skysongdev.skysongEconomy.business.module;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.scale.BusinessScale;
import org.skysongdev.skysongEconomy.business.scale.SkillSections;
import org.skysongdev.skysongEconomy.config.FileRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * {@code businesses/modules.yml} and the module files it names.
 *
 * @param skills  the skills that give module picks
 * @param modules every module file that loaded
 */
public record ModuleSettings(SkillSections<ModuleSkillSettings> skills, FileRegistry<Module> modules) {
    /** No skill gives module picks. */
    public static final ModuleSettings NONE = new ModuleSettings(SkillSections.none(), FileRegistry.empty());

    /**
     * @return how many module picks each skill gives the business now, in file order; only
     *         skills the business has picked
     */
    public Map<String, Integer> totals(Business business) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        skills.all().forEach((skill, settings) -> {
            if (business.pickCount(skill) > 0) {
                totals.put(skill, settings.picks().at(BusinessScale.of(business, skill)));
            }
        });
        return totals;
    }

    /**
     * @return how many module picks the skill gives the business now: 0 when the business has
     *         not picked the skill, empty when the file does not list the skill at all
     */
    public OptionalInt total(Business business, String skill) {
        Optional<ModuleSkillSettings> settings = skills.of(skill);
        if (settings.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(business.pickCount(skill) > 0
                ? settings.get().picks().at(BusinessScale.of(business, skill)) : 0);
    }

    /** @return the modules a skill's picks can take, in file order; none when the skill is not listed */
    public List<Module> pool(String skill) {
        return skills.of(skill).map(settings -> settings.pool().stream()
                .flatMap(id -> modules.find(id).stream()).toList()).orElse(List.of());
    }
}
