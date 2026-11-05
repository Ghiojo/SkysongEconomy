package org.skysongdev.skysongEconomy.business.type;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every business type the server defines, by id. Each skill belongs to at most one type.
 */
public final class BusinessTypeRegistry {
    private final Map<String, BusinessType> byId;
    private final Map<String, BusinessType> bySkill;
    private final List<BusinessType> ordered;

    /**
     * @throws IllegalArgumentException when two types share an id or a skill
     */
    public BusinessTypeRegistry(Collection<BusinessType> types) {
        Map<String, BusinessType> ids = new LinkedHashMap<>();
        Map<String, BusinessType> skills = new LinkedHashMap<>();
        for (BusinessType type : types) {
            if (ids.putIfAbsent(type.id(), type) != null) {
                throw new IllegalArgumentException("Duplicate business type " + type.id());
            }
            for (String skill : type.skillIds()) {
                BusinessType other = skills.putIfAbsent(skill, type);
                if (other != null) {
                    throw new IllegalArgumentException("Skill " + skill + " is in both " + other.id() + " and " + type.id());
                }
            }
        }
        this.byId = Map.copyOf(ids);
        this.bySkill = Map.copyOf(skills);
        this.ordered = List.copyOf(ids.values());
    }

    /** @return the type with that id */
    public Optional<BusinessType> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** @return the type a skill belongs to, empty when no type offers it */
    public Optional<BusinessType> typeOfSkill(String skillId) {
        return Optional.ofNullable(bySkill.get(skillId));
    }

    /** @return every type, in the order of the config file */
    public List<BusinessType> all() {
        return ordered;
    }
}
