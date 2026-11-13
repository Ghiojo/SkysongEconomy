package org.skysongdev.skysongEconomy.business.scale;

import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * The {@code skills:} map of one functionality's config file: which skills have the
 * functionality, and each one's settings.
 * <pre>
 * skills:
 *   martial:
 *     ...settings of this functionality for martial...
 *   economical:
 *     ...
 * </pre>
 * A skill listed there has the functionality; a skill left out does not. Every functionality
 * keeps its own file, so listing a skill in one never affects another, and a skill can have any
 * combination of them.
 *
 * @param <T> the settings one skill has for this functionality
 */
public final class SkillSections<T> {
    /** The key of the map in the file. */
    public static final String SKILLS = "skills";

    private final Map<String, T> bySkill;

    private SkillSections(Map<String, T> bySkill) {
        this.bySkill = Collections.unmodifiableMap(bySkill);
    }

    /** @return a functionality no skill has */
    public static <T> SkillSections<T> none() {
        return new SkillSections<>(Map.of());
    }

    /**
     * Reads the {@code skills:} map of a file.
     *
     * @param root     the file's root section
     * @param skills   checks that every listed skill exists in SkysongStats
     * @param warnings told about every skill that was skipped because it doesn't exist
     * @param settings reads one skill's settings, given the skill id (lower case) and its section
     * @param <T>      the settings one skill has
     * @return the skills that have the functionality, in file order
     * @throws ConfigException when {@code skills} isn't a map of sections, or a skill's settings
     *                         can't be read
     */
    public static <T> SkillSections<T> read(ConfigSection root, SkillCatalog skills, Consumer<String> warnings,
                                            BiFunction<String, ConfigSection, T> settings) {
        ConfigSection section = root.section(SKILLS);
        Map<String, T> bySkill = new LinkedHashMap<>();
        for (String written : section.keys()) {
            String skillId = written.toLowerCase(Locale.ROOT);
            if (!skills.exists(skillId)) {
                warnings.accept(section.path() + ": unknown skill '" + written + "' was skipped");
                continue;
            }
            if (bySkill.containsKey(skillId)) {
                throw section.error(written, "is listed twice");
            }
            bySkill.put(skillId, settings.apply(skillId, section.section(written)));
        }
        return new SkillSections<>(bySkill);
    }

    /** @return the skills that have the functionality, in file order */
    public Set<String> skills() {
        return bySkill.keySet();
    }

    /** @return whether that skill has the functionality */
    public boolean has(String skillId) {
        return bySkill.containsKey(skillId);
    }

    /** @return that skill's settings, empty when it doesn't have the functionality */
    public Optional<T> of(String skillId) {
        return Optional.ofNullable(bySkill.get(skillId));
    }

    /** @return every skill's settings by skill id, in file order, unmodifiable */
    public Map<String, T> all() {
        return bySkill;
    }
}
