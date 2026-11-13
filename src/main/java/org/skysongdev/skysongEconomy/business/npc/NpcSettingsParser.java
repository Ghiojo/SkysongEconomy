package org.skysongdev.skysongEconomy.business.npc;

import org.skysongdev.skysongEconomy.business.npc.benefit.BenefitParser;
import org.skysongdev.skysongEconomy.business.npc.benefit.BenefitSources;
import org.skysongdev.skysongEconomy.business.npc.benefit.Progression;
import org.skysongdev.skysongEconomy.business.scale.AmountParser;
import org.skysongdev.skysongEconomy.business.scale.SkillSections;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.function.Consumer;

/**
 * Reads {@code businesses/npcs.yml}:
 * <pre>{@code
 * spawn-at: player        # or business
 * spawn-radius: 3
 * skills:
 *   martial:
 *     slots: "{level}"                    # a number, a formula, or a table
 *     template: business_guard            # a SkysongNPCs template
 *     progression:                        # optional: what each NPC receives, see BenefitParser
 *       stat-points: {amount: "3 + {level}"}
 *   economical:
 *     slots: {by: level, 1: 1, 3: 2, 5: 3}
 *     template: business_clerk
 * }</pre>
 * A skill listed under {@code skills} gives its businesses NPCs; listing it here changes nothing
 * about any other functionality the skill has. Unknown skills are skipped with a warning.
 */
public final class NpcSettingsParser {
    /** File name used in error messages and when loading. */
    public static final String FILE = "businesses/npcs.yml";

    private final SkillCatalog skills;
    private final BenefitParser benefits;
    private final Consumer<String> warnings;

    /**
     * @param benefits reads each skill's {@code progression}
     * @param warnings told about skills that are skipped, as console lines
     */
    public NpcSettingsParser(SkillCatalog skills, BenefitParser benefits, Consumer<String> warnings) {
        this.skills = skills;
        this.benefits = benefits;
        this.warnings = warnings;
    }

    /**
     * @throws ConfigException when a value is missing, out of range or not written correctly
     */
    public NpcSettings parse(ConfigSection root) {
        String writtenPlace = root.string("spawn-at", SpawnAt.PLAYER.key());
        SpawnAt spawnAt = SpawnAt.fromKey(writtenPlace)
                .orElseThrow(() -> root.error("spawn-at", "is '" + writtenPlace + "': use player or business"));
        int radius = root.integer("spawn-radius", NpcSettings.DEFAULT_SPAWN_RADIUS);
        if (radius < 0) {
            throw root.error("spawn-radius", "must be 0 or more");
        }
        return new NpcSettings(spawnAt, radius, SkillSections.read(root, skills, warnings, this::skill));
    }

    private NpcSkillSettings skill(String skillId, ConfigSection section) {
        String template = section.requireString("template").trim();
        if (template.isEmpty()) {
            throw section.error("template", "must name a SkysongNPCs template");
        }
        if (!BenefitSources.fits(SlotSource.skill(skillId))) {
            throw section.error("template", "belongs to a skill whose id is too long for SkysongNPCs to track what it gives");
        }
        Progression progression = section.contains("progression")
                ? benefits.progression(section.section("progression")) : Progression.NONE;
        return new NpcSkillSettings(AmountParser.require(section, "slots"), template, progression);
    }
}
