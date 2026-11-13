package org.skysongdev.skysongEconomy.business.module.effect;

import org.skysongdev.skysongEconomy.business.npc.SlotSource;
import org.skysongdev.skysongEconomy.business.npc.benefit.BenefitParser;
import org.skysongdev.skysongEconomy.business.npc.benefit.BenefitSources;
import org.skysongdev.skysongEconomy.business.npc.benefit.Progression;
import org.skysongdev.skysongEconomy.business.scale.AmountParser;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every effect type a module file can use, by its {@code type}. Adding an effect means adding a
 * {@link ModuleEffect} record, its parser, and one {@link #register} line where the registry is
 * built.
 * <pre>
 * effects:
 *   - type: npc-slots
 *     amount: 1
 *     template: war_hound
 *     progression: {stats: {strength: 2}}
 *   - type: npc-benefit
 *     target: all
 *     from: martial
 *     benefit: {stat-points: {amount: 2}}
 *   - type: worker-slots
 *     amount: 1
 * </pre>
 */
public final class ModuleEffectParsers {
    /** What {@code from:} says for the NPCs of the module's own slots. */
    public static final String FROM_MODULE = "module";
    /** What {@code target:} says for every NPC that counts. */
    public static final String TARGET_ALL = "all";

    private final Map<String, ModuleEffectParser> byType = new LinkedHashMap<>();

    /** @return a registry that knows {@code worker-slots}, which needs no other plugin */
    public static ModuleEffectParsers basic() {
        ModuleEffectParsers parsers = new ModuleEffectParsers();
        parsers.register(ModuleEffect.WorkerSlots.TYPE,
                (moduleId, section) -> new ModuleEffect.WorkerSlots(AmountParser.require(section, "amount")));
        return parsers;
    }

    /**
     * Adds the NPC effects, read in full. Use it when SkysongNPCs is installed.
     *
     * @param benefits reads and checks benefit blocks
     * @param skills   checks the skill a {@code from:} names
     */
    public ModuleEffectParsers withNpcEffects(BenefitParser benefits, SkillCatalog skills) {
        register(ModuleEffect.NpcSlots.TYPE, (moduleId, section) -> npcSlots(moduleId, section, benefits));
        register(ModuleEffect.NpcBenefit.TYPE, (moduleId, section) -> npcBenefit(moduleId, section, benefits, skills));
        return this;
    }

    /**
     * Adds the NPC effects as ones this server can't apply. Use it when SkysongNPCs is not
     * installed, so modules that have them still load.
     */
    public ModuleEffectParsers withoutNpcEffects() {
        register(ModuleEffect.NpcSlots.TYPE, (moduleId, section) -> new ModuleEffect.Unavailable(ModuleEffect.NpcSlots.TYPE));
        register(ModuleEffect.NpcBenefit.TYPE, (moduleId, section) -> new ModuleEffect.Unavailable(ModuleEffect.NpcBenefit.TYPE));
        return this;
    }

    /** Adds an effect type, or replaces the one with that name. */
    public void register(String type, ModuleEffectParser parser) {
        byType.put(type, parser);
    }

    /**
     * Reads a module's {@code effects} list.
     *
     * @param root the module file's root section
     * @return the effects in file order; none when the key is missing
     * @throws ConfigException when an effect has no known {@code type}, can't be read, or the
     *                         list has two {@code npc-slots} effects or a {@code from: module}
     *                         without one
     */
    public List<ModuleEffect> parse(String moduleId, ConfigSection root) {
        List<ModuleEffect> effects = new ArrayList<>();
        for (ConfigSection section : root.sections("effects")) {
            String type = section.requireString("type").strip().toLowerCase(Locale.ROOT);
            ModuleEffectParser parser = byType.get(type);
            if (parser == null) {
                throw section.error("type", "'" + type + "' isn't an effect: use " + String.join(", ", byType.keySet()));
            }
            effects.add(parser.parse(moduleId, section));
        }
        long slotEffects = effects.stream().filter(ModuleEffect.NpcSlots.class::isInstance).count();
        if (slotEffects > 1) {
            throw root.error("effects", "has more than one " + ModuleEffect.NpcSlots.TYPE
                    + " effect; a module has one kind of NPC of its own, so put its slots in one effect");
        }
        SlotSource own = SlotSource.module(moduleId);
        boolean targetsOwn = effects.stream().anyMatch(effect -> effect instanceof ModuleEffect.NpcBenefit benefit
                && own.equals(benefit.target().from()));
        if (targetsOwn && slotEffects == 0) {
            throw root.error("effects", "has a benefit with from: " + FROM_MODULE + ", but the module has no "
                    + ModuleEffect.NpcSlots.TYPE + " effect to give it NPCs of its own");
        }
        return effects;
    }

    private static ModuleEffect npcSlots(String moduleId, ConfigSection section, BenefitParser benefits) {
        String template = section.requireString("template").strip();
        if (template.isEmpty()) {
            throw section.error("template", "must name a SkysongNPCs template");
        }
        requireFits(moduleId, section);
        Progression progression = section.contains("progression")
                ? benefits.progression(section.section("progression")) : Progression.NONE;
        return new ModuleEffect.NpcSlots(AmountParser.require(section, "amount"), template, progression);
    }

    private static ModuleEffect npcBenefit(String moduleId, ConfigSection section, BenefitParser benefits, SkillCatalog skills) {
        requireFits(moduleId, section);
        if (!section.contains("benefit")) {
            throw section.error("benefit", "is required: what the NPCs receive");
        }
        return new ModuleEffect.NpcBenefit(new NpcTarget(count(section), from(moduleId, section, skills)),
                benefits.benefit(section.section("benefit")));
    }

    private static int count(ConfigSection section) {
        Object raw = section.raw("target");
        if (raw == null || TARGET_ALL.equalsIgnoreCase(raw.toString().strip())) {
            return NpcTarget.ALL;
        }
        try {
            int count = Integer.parseInt(raw.toString().strip());
            if (count >= 1) {
                return count;
            }
        } catch (NumberFormatException e) {
            // Falls through to the error below.
        }
        throw section.error("target", "must be " + TARGET_ALL + " or how many NPCs the owner chooses, 1 or more");
    }

    private static SlotSource from(String moduleId, ConfigSection section, SkillCatalog skills) {
        String written = section.string("from", "").strip().toLowerCase(Locale.ROOT);
        if (written.isEmpty()) {
            return null;
        }
        if (written.equals(FROM_MODULE)) {
            return SlotSource.module(moduleId);
        }
        if (!skills.exists(written)) {
            throw section.error("from", "must be a skill id or " + FROM_MODULE + "; there is no skill '" + written + "'");
        }
        return SlotSource.skill(written);
    }

    private static void requireFits(String moduleId, ConfigSection section) {
        if (!BenefitSources.fits(SlotSource.module(moduleId))) {
            throw section.error("type", "belongs to a module whose file name is too long for SkysongNPCs to track what it gives");
        }
    }
}
