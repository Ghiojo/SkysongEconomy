package org.skysongdev.skysongEconomy.business.module.effect;

import org.skysongdev.skysongEconomy.business.npc.benefit.Benefit;
import org.skysongdev.skysongEconomy.business.npc.benefit.Progression;
import org.skysongdev.skysongEconomy.business.scale.Amount;

import java.util.Objects;

/**
 * One thing a picked module does. A module can have several, and all of them apply together.
 * Every amount is worked out at the level of the skill the module was picked for and the
 * business's tier.
 */
public sealed interface ModuleEffect {
    /** @return the effect's {@code type} in a module file */
    String type();

    /**
     * {@code npc-slots}: NPC slots of the module's own kind. Their NPCs use this template and
     * progression, not any skill's NPC settings.
     *
     * @param amount      how many slots
     * @param template    the SkysongNPCs template the NPCs are created from
     * @param progression what each of them receives
     */
    record NpcSlots(Amount amount, String template, Progression progression) implements ModuleEffect {
        /** The {@code type} in a module file. */
        public static final String TYPE = "npc-slots";

        public NpcSlots {
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(template, "template");
            Objects.requireNonNull(progression, "progression");
        }

        @Override
        public String type() {
            return TYPE;
        }
    }

    /**
     * {@code npc-benefit}: a benefit for NPCs of the business.
     *
     * @param target  which NPCs receive it
     * @param benefit what each of them receives
     */
    record NpcBenefit(NpcTarget target, Benefit benefit) implements ModuleEffect {
        /** The {@code type} in a module file. */
        public static final String TYPE = "npc-benefit";

        public NpcBenefit {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(benefit, "benefit");
        }

        @Override
        public String type() {
            return TYPE;
        }
    }

    /**
     * {@code worker-slots}: more workers than the business's tier allows.
     *
     * @param amount how many more
     */
    record WorkerSlots(Amount amount) implements ModuleEffect {
        /** The {@code type} in a module file. */
        public static final String TYPE = "worker-slots";

        public WorkerSlots {
            Objects.requireNonNull(amount, "amount");
        }

        @Override
        public String type() {
            return TYPE;
        }
    }

    /**
     * An effect this server can't apply because a plugin it needs is missing: the NPC effects
     * without SkysongNPCs. The module can still be picked; the effect does nothing.
     *
     * @param type the effect's {@code type} in the module file
     */
    record Unavailable(String type) implements ModuleEffect {
    }
}
