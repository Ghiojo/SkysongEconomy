package org.skysongdev.skysongEconomy.business.scale;

import org.skysongdev.skysongEconomy.business.Business;

/**
 * How far a business has come, as seen from one of its skills.
 *
 * @param level how many of the business's skill picks are that skill, 0 when it has none
 * @param tier  the business's overall tier
 */
public record BusinessScale(int level, int tier) {

    /**
     * @throws IllegalArgumentException when the level is negative or the tier isn't a business tier
     */
    public BusinessScale {
        if (level < 0) {
            throw new IllegalArgumentException("A level is 0 or more, not " + level);
        }
        if (tier < Business.MIN_TIER || tier > Business.MAX_TIER) {
            throw new IllegalArgumentException("A tier is " + Business.MIN_TIER + " to " + Business.MAX_TIER + ", not " + tier);
        }
    }

    /**
     * @param business the business
     * @param skillId  the skill whose level counts
     * @return the business's scale for that skill
     */
    public static BusinessScale of(Business business, String skillId) {
        return new BusinessScale(business.pickCount(skillId), business.tier());
    }

    /** @return the number a table or formula on that basis looks at */
    public int value(Basis basis) {
        return basis == Basis.LEVEL ? level : tier;
    }
}
