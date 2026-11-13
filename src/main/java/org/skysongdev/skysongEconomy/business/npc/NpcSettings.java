package org.skysongdev.skysongEconomy.business.npc;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.scale.BusinessScale;
import org.skysongdev.skysongEconomy.business.scale.SkillSections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The business NPC rules, from {@code businesses/npcs.yml}.
 *
 * @param spawnAt     where a new NPC appears
 * @param spawnRadius how many blocks around that point SkysongNPCs looks for a free spot
 * @param skills      the skills that give NPCs, each with its settings
 */
public record NpcSettings(SpawnAt spawnAt, int spawnRadius, SkillSections<NpcSkillSettings> skills) {
    /** The search radius when the file doesn't set one. */
    public static final int DEFAULT_SPAWN_RADIUS = 3;

    public NpcSettings {
        Objects.requireNonNull(spawnAt, "spawnAt");
        Objects.requireNonNull(skills, "skills");
        if (spawnRadius < 0) {
            throw new IllegalArgumentException("spawnRadius must not be negative: " + spawnRadius);
        }
    }

    /** @return settings under which no skill gives NPCs */
    public static NpcSettings none() {
        return new NpcSettings(SpawnAt.PLAYER, DEFAULT_SPAWN_RADIUS, SkillSections.none());
    }

    /**
     * @return how many NPC slots each source gives the business right now, in file order. A
     *         skill the business never picked is left out, as it gives nothing.
     */
    public Map<SlotSource, Integer> capacities(Business business) {
        Map<SlotSource, Integer> capacities = new LinkedHashMap<>();
        skills.all().forEach((skillId, settings) -> {
            if (business.pickCount(skillId) > 0) {
                capacities.put(SlotSource.skill(skillId), settings.slots().at(BusinessScale.of(business, skillId)));
            }
        });
        return capacities;
    }

    /**
     * @return how many slots that source gives the business right now; empty when the file no
     *         longer lists the source, so nothing can be said about it
     */
    public OptionalInt capacity(Business business, SlotSource source) {
        if (!source.isSkill() || !skills.has(source.id())) {
            return OptionalInt.empty();
        }
        Integer capacity = capacities(business).get(source);
        return OptionalInt.of(capacity == null ? 0 : capacity);
    }

    /** @return the SkysongNPCs template the source's NPCs are created from, empty when the source gives no NPCs */
    public Optional<String> template(SlotSource source) {
        if (!source.isSkill()) {
            return Optional.empty();
        }
        return skills.of(source.id()).map(NpcSkillSettings::template);
    }
}
