package org.skysongdev.skysongEconomy.business.npc;

import org.skysongdev.skysongEconomy.business.npc.benefit.Progression;
import org.skysongdev.skysongEconomy.business.scale.Amount;

import java.util.Objects;

/**
 * What one skill's entry in {@code businesses/npcs.yml} says about its NPCs.
 *
 * @param slots       how many NPCs the skill gives, by skill pick tier or business tier
 * @param template    the SkysongNPCs template its NPCs are created from: entity, default name and
 *                    skin, starting stats and ancestries
 * @param progression what each of its NPCs receives as the business grows
 */
public record NpcSkillSettings(Amount slots, String template, Progression progression) {
    public NpcSkillSettings {
        Objects.requireNonNull(slots, "slots");
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(progression, "progression");
    }
}
