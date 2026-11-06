package org.skysongdev.skysongEconomy.nodes.farm;

import org.skysongdev.skysongEconomy.config.FileEntry;

import java.util.List;
import java.util.Objects;

/**
 * A kind of farm node, such as a crop plot or an animal pen, from
 * {@code nodes/farm/types/<id>.yml}. A farm node is created with one type and only takes that
 * type's resources.
 *
 * @param id        the file name, which farm nodes refer to and boosters match against
 * @param name      what players see, in MiniMessage
 * @param skill     the SkysongStats skill every harvest on this type checks
 * @param resources the ids of the resources it accepts, in the file's order; each one loaded
 */
public record FarmType(String id, String name, String skill, List<String> resources) implements FileEntry {
    public FarmType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(skill, "skill");
        resources = List.copyOf(resources);
    }

    /** @return whether a farm node of this type can take that resource */
    public boolean accepts(String resourceId) {
        return resources.contains(resourceId);
    }
}
