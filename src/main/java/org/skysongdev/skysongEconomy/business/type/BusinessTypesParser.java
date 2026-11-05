package org.skysongdev.skysongEconomy.business.type;

import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Reads {@code businesses/types.yml}:
 * <pre>{@code
 * types:
 *   gathering:
 *     name: "<green>Gathering"
 *     skills: [farming, mining]
 *     trains-proficiency: false
 * }</pre>
 * Unknown skills, and skills already claimed by an earlier type, are reported and left out
 * instead of failing the whole file, so one typo can't disable every business.
 */
public final class BusinessTypesParser {
    /** File name used in error messages and when loading. */
    public static final String FILE = "businesses/types.yml";

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]+");

    private final SkillCatalog skills;
    private final Consumer<String> warnings;

    /**
     * @param skills   checks that every listed skill exists in SkysongStats
     * @param warnings receives one message per problem that was skipped
     */
    public BusinessTypesParser(SkillCatalog skills, Consumer<String> warnings) {
        this.skills = skills;
        this.warnings = warnings;
    }

    /**
     * @throws ConfigException when a type is unusable, e.g. it has a bad id or no name
     */
    public BusinessTypeRegistry parse(ConfigSection root) {
        ConfigSection types = root.section("types");
        if (types.keys().isEmpty()) {
            warnings.accept(types.path() + " defines no business types");
        }
        Map<String, String> claimedBy = new HashMap<>();
        List<BusinessType> parsed = new ArrayList<>();
        for (String id : types.keys()) {
            if (!ID.matcher(id).matches()) {
                throw types.error(id, "is not a valid id: use lower case letters, digits, '_' and '-'");
            }
            parsed.add(parseType(id, types.section(id), claimedBy));
        }
        return new BusinessTypeRegistry(parsed);
    }

    private BusinessType parseType(String id, ConfigSection section, Map<String, String> claimedBy) {
        String name = section.requireString("name");
        Set<String> typeSkills = new LinkedHashSet<>();
        for (String raw : section.strings("skills")) {
            String skill = raw.toLowerCase(Locale.ROOT);
            if (!skills.exists(skill)) {
                warnings.accept(section.path() + ".skills: unknown skill '" + raw + "' was skipped");
                continue;
            }
            String owner = claimedBy.putIfAbsent(skill, id);
            if (owner != null && !owner.equals(id)) {
                warnings.accept(section.path() + ".skills: '" + skill + "' already belongs to " + owner + " and was skipped");
                continue;
            }
            typeSkills.add(skill);
        }
        if (typeSkills.isEmpty()) {
            warnings.accept(section.path() + " has no usable skills, so its businesses can't pick any");
        }
        return new BusinessType(id, name, typeSkills, section.bool("trains-proficiency", false));
    }
}
