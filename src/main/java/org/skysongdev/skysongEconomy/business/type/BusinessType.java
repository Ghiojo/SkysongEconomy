package org.skysongdev.skysongEconomy.business.type;

import java.util.Objects;
import java.util.Set;

/**
 * A kind of business, such as Gathering, Crafting or Service. The type decides which skills
 * the owner may pick and which pay table its shifts use.
 *
 * @param id                 the key in {@code businesses/types.yml}, lower case
 * @param displayName        the name shown to players, in MiniMessage
 * @param skillIds           the SkysongStats skills a business of this type may pick
 * @param trainsProficiency  whether training here also trains the player's proficiency focus
 */
public record BusinessType(String id, String displayName, Set<String> skillIds, boolean trainsProficiency) {
    public BusinessType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        skillIds = Set.copyOf(skillIds);
    }

    /** @return whether a business of this type may pick the skill */
    public boolean offers(String skillId) {
        return skillIds.contains(skillId);
    }
}
