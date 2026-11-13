package org.skysongdev.skysongEconomy.business.npc.benefit;

import java.util.List;

/**
 * What bringing an NPC in line changed.
 *
 * @param pointsAdded          stat points given
 * @param picksAdded           proficiency picks given
 * @param abilityChoicesAdded  ability choices given
 * @param choicesReset         whether everything the business gave was given back to choose again,
 *                             because the NPC is owed less than before
 * @param picksReset           whether its proficiency picks were given back to pick again,
 *                             because how far they may be promoted changed
 * @param abilitiesGranted     fixed abilities given
 * @param abilitiesRevoked     fixed abilities taken away
 * @param statsChanged         baked stat bonuses added, changed or removed
 * @param proficienciesChanged fixed proficiencies raised or put back
 */
public record Changes(int pointsAdded, int picksAdded, int abilityChoicesAdded, boolean choicesReset, boolean picksReset,
                      List<String> abilitiesGranted, List<String> abilitiesRevoked, int statsChanged,
                      int proficienciesChanged) {
    /** Nothing changed. */
    public static final Changes NONE = new Changes(0, 0, 0, false, false, List.of(), List.of(), 0, 0);

    public Changes {
        abilitiesGranted = List.copyOf(abilitiesGranted);
        abilitiesRevoked = List.copyOf(abilitiesRevoked);
    }

    /** @return whether anything changed */
    public boolean any() {
        return !equals(NONE);
    }

    /** @return whether the owner has something to choose because of it */
    public boolean leavesChoices() {
        return pointsAdded > 0 || picksAdded > 0 || abilityChoicesAdded > 0 || choicesReset || picksReset;
    }
}
