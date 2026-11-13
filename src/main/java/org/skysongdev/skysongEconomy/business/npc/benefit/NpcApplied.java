package org.skysongdev.skysongEconomy.business.npc.benefit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * What the business system changed on one NPC's SkysongStats sheet. Unlike stat points, picks
 * and abilities, the sheet does not record who changed it, so this is remembered here in order
 * to undo exactly that and nothing else.
 *
 * @param stats         the baked stat bonuses it added, by stat id
 * @param proficiencies the proficiencies it raised, by proficiency id
 */
public record NpcApplied(Map<String, Stat> stats, Map<String, Proficiency> proficiencies) {
    /** Nothing was changed on the sheet. */
    public static final NpcApplied NONE = new NpcApplied(Map.of(), Map.of());

    public NpcApplied {
        stats = Collections.unmodifiableMap(new LinkedHashMap<>(stats));
        proficiencies = Collections.unmodifiableMap(new LinkedHashMap<>(proficiencies));
    }

    /**
     * A baked stat bonus.
     *
     * @param modifierId the id SkysongStats gave the modifier, to remove it with
     * @param amount     the bonus
     */
    public record Stat(String modifierId, int amount) {
        public Stat {
            Objects.requireNonNull(modifierId, "modifierId");
        }
    }

    /**
     * A proficiency that was raised.
     *
     * @param before  the tier the NPC had before, which it goes back to
     * @param applied the tier it was raised to
     */
    public record Proficiency(String before, String applied) {
        public Proficiency {
            Objects.requireNonNull(before, "before");
            Objects.requireNonNull(applied, "applied");
        }
    }

    /** @return whether nothing was changed on the sheet */
    public boolean isEmpty() {
        return stats.isEmpty() && proficiencies.isEmpty();
    }
}
