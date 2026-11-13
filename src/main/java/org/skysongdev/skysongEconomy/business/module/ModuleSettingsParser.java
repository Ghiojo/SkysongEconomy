package org.skysongdev.skysongEconomy.business.module;

import org.skysongdev.skysongEconomy.business.scale.AmountParser;
import org.skysongdev.skysongEconomy.business.scale.SkillSections;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.FileRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Reads {@code businesses/modules.yml}.
 * <pre>
 * skills:
 *   martial:
 *     picks: {by: level, 1: 1, 3: 2, 5: 3}
 *     modules: [kennel, veteran_sergeants, sworn_oath]
 * </pre>
 * {@code picks} is the total number of module picks, an amount that can follow the skill's
 * level or the business's tier. {@code modules} names files in {@code modules/}; a name with no
 * file is left out of the pool with a warning.
 */
public final class ModuleSettingsParser {
    /** The file, relative to the plugin's data folder. */
    public static final String FILE = "businesses/modules.yml";

    private final SkillCatalog skills;
    private final Consumer<String> warnings;

    /**
     * @param skills   checks that every listed skill exists in SkysongStats
     * @param warnings told about unknown skills and modules, which are skipped
     */
    public ModuleSettingsParser(SkillCatalog skills, Consumer<String> warnings) {
        this.skills = skills;
        this.warnings = warnings;
    }

    /**
     * @param root    the root of {@code businesses/modules.yml}
     * @param modules the module files that loaded
     * @throws ConfigException when a skill's settings can't be read
     */
    public ModuleSettings parse(ConfigSection root, FileRegistry<Module> modules) {
        if (!root.contains(SkillSections.SKILLS)) {
            return new ModuleSettings(SkillSections.none(), modules);
        }
        return new ModuleSettings(SkillSections.read(root, skills, warnings, (skill, section) -> {
            List<String> pool = new ArrayList<>();
            for (String written : section.strings("modules")) {
                String id = written.strip().toLowerCase(Locale.ROOT);
                if (modules.find(id).isEmpty()) {
                    warnings.accept(section.path() + ".modules: there is no module '" + written + "' in the "
                            + ModuleParser.FOLDER + " folder, so it was left out");
                } else if (!pool.contains(id)) {
                    pool.add(id);
                }
            }
            return new ModuleSkillSettings(AmountParser.require(section, "picks"), pool);
        }), modules);
    }
}
