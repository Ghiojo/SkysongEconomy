package org.skysongdev.skysongEconomy.business.npc.benefit;

import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.business.npc.SlotSource;

/**
 * The names under which the business system gives things to NPCs in SkysongNPCs. Everything it
 * gives carries one, so it can be counted and taken back without touching what a template or
 * staff gave.
 */
public final class BenefitSources {
    private static final String PREFIX = "economy:";
    private static final String PICKS = ":picks";

    private BenefitSources() {
    }

    /** @return the source for what a slot's own progression gives, such as {@code economy:skill:martial} */
    public static String of(SlotSource slot) {
        return PREFIX + slot.key();
    }

    /**
     * @return the source for the proficiency picks of a giver, kept apart so picks can be reset
     *         without resetting stat points: {@code economy:skill:martial:picks}
     */
    public static String picks(String source) {
        return source + PICKS;
    }

    /** @return whether SkysongNPCs accepts the slot's sources: no spaces and short enough */
    public static boolean fits(SlotSource slot) {
        String longest = picks(of(slot));
        return longest.length() <= NpcSheetGateway.MAX_SOURCE_LENGTH && longest.chars().noneMatch(Character::isWhitespace);
    }
}
