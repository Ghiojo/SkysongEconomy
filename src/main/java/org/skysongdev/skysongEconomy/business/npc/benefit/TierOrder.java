package org.skysongdev.skysongEconomy.business.npc.benefit;

import java.util.List;
import java.util.Locale;

/**
 * The proficiency tiers, lowest first (untrained, proficient, adept, master in SkysongStats), so
 * tiers can be compared.
 *
 * @param ids the tier ids, lowest first
 */
public record TierOrder(List<String> ids) {
    public TierOrder {
        ids = ids.stream().map(id -> id.toLowerCase(Locale.ROOT)).toList();
    }

    /** @return whether there is a tier with that id */
    public boolean contains(String tier) {
        return rank(tier) >= 0;
    }

    /** @return the tier's place from the lowest (0), or -1 for an id that is not a tier */
    public int rank(String tier) {
        return ids.indexOf(tier.toLowerCase(Locale.ROOT));
    }

    /** @return whether {@code tier} is above {@code other}; an unknown tier is below every tier */
    public boolean isAbove(String tier, String other) {
        return rank(tier) > rank(other);
    }

    /** @return the higher of two tiers */
    public String higher(String first, String second) {
        return isAbove(second, first) ? second : first;
    }

    /** @return the lowest tier that is worth giving: the one right above the lowest (proficient) */
    public String firstTrained() {
        return ids.size() > 1 ? ids.get(1) : ids.get(0);
    }
}
