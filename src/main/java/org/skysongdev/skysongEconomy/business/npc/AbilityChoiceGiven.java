package org.skysongdev.skysongEconomy.business.npc;

import java.util.List;

/**
 * A choice of abilities one source gave an NPC: it chooses {@code count} of {@code options}.
 *
 * @param options the ability ids to choose from
 * @param count   how many it may choose
 * @param chosen  the ones chosen so far
 */
public record AbilityChoiceGiven(List<String> options, int count, List<String> chosen) {
    public AbilityChoiceGiven {
        options = List.copyOf(options);
        chosen = List.copyOf(chosen);
    }

    public int remaining() {
        return Math.max(0, count - chosen.size());
    }
}
