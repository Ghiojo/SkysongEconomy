package org.skysongdev.skysongEconomy.nodes.farm;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.FileRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reads one farm type file:
 * <pre>{@code
 * name: "<gold>Animal pen"          # what players see; default the file name
 * skill: animalhandling             # the skill every harvest on this type checks
 * resources: [sheep, cow, chicken]  # files in nodes/farm/resources
 * }</pre>
 * A listed resource whose file is missing or didn't load is left out with a warning, so one
 * broken resource doesn't take its whole type down.
 */
public final class FarmTypeParser {
    /** The folder, relative to the plugin's data folder. */
    public static final String FOLDER = "nodes/farm/types";

    private final Predicate<String> skillExists;
    private final Consumer<String> warnings;

    /**
     * @param skillExists whether a skill id exists in SkysongStats
     * @param warnings    told about every resource that is left out, and about primers two
     *                    resources share
     */
    public FarmTypeParser(Predicate<String> skillExists, Consumer<String> warnings) {
        this.skillExists = skillExists;
        this.warnings = warnings;
    }

    /**
     * @param id        the file name without {@code .yml}
     * @param resources the farm resources that loaded
     * @throws ConfigException when anything in the file is missing or wrong
     */
    public FarmType parse(String id, ConfigSection root, FileRegistry<FarmResource> resources) {
        String skill = root.requireString("skill").strip().toLowerCase(Locale.ROOT);
        if (!skillExists.test(skill)) {
            throw root.error("skill", "'" + skill + "' isn't a skill in SkysongStats");
        }
        Set<String> listed = new LinkedHashSet<>();
        root.strings("resources").forEach(resource -> listed.add(resource.strip().toLowerCase(Locale.ROOT)));
        if (listed.isEmpty()) {
            throw root.error("resources", "must list at least one file from " + FarmResourceParser.FOLDER);
        }
        List<FarmResource> accepted = new ArrayList<>();
        for (String resource : listed) {
            resources.find(resource).ifPresentOrElse(accepted::add, () -> warnings.accept("The farm type '" + id
                    + "' lists the resource '" + resource + "', which is missing or didn't load, so it is left out"));
        }
        warnAboutSharedPrimers(id, accepted);
        return new FarmType(id, root.string("name", id), skill, accepted.stream().map(FarmResource::id).toList());
    }

    /** An item without the {@code node_primer} tag plants the first resource that names it. */
    private void warnAboutSharedPrimers(String id, List<FarmResource> resources) {
        Map<String, String> firstByItem = new HashMap<>();
        for (FarmResource resource : resources) {
            String item = resource.primer().materialId();
            String first = firstByItem.putIfAbsent(item, resource.id());
            if (first != null) {
                warnings.accept("In the farm type '" + id + "', the resources '" + first + "' and '" + resource.id()
                        + "' both use the primer item '" + item + "'. Without a node_primer tag, it plants '" + first
                        + "'.");
            }
        }
    }
}
