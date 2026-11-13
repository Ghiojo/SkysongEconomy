package org.skysongdev.skysongEconomy.business.module;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleEffect;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleEffectParsers;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Reads one module file in {@code modules/}.
 * <pre>
 * name: "&lt;white&gt;Sworn Oath"
 * icon: PAPER
 * description:
 *   - "&lt;gray&gt;Your guards are bound by oath."
 * max-picks: 1
 * requires:
 *   tier: 2
 *   level: 1
 *   modules: [kennel]
 * </pre>
 * Only {@code name} is required. {@code description} is one text or a list of lines. An optional
 * {@code effects} list says what the module does once picked; see {@link ModuleEffectParsers}.
 */
public final class ModuleParser {
    /** The folder, relative to the plugin's data folder. */
    public static final String FOLDER = "modules";

    private static final String DEFAULT_ICON = "PAPER";

    private final ModuleEffectParsers effects;
    private final Consumer<String> warnings;

    /**
     * @param effects  reads the {@code effects} list
     * @param warnings told about effects this server can't apply
     */
    public ModuleParser(ModuleEffectParsers effects, Consumer<String> warnings) {
        this.effects = effects;
        this.warnings = warnings;
    }

    /**
     * @param id   the file name without {@code .yml}
     * @param root the file's root section
     * @throws ConfigException when {@code name} is missing or a value can't be read
     */
    public Module parse(String id, ConfigSection root) {
        int maxPicks = root.integer("max-picks", 1);
        if (maxPicks < 1) {
            throw root.error("max-picks", "must be 1 or more");
        }
        List<ModuleEffect> effects = this.effects.parse(id, root);
        effects.stream().filter(ModuleEffect.Unavailable.class::isInstance).map(ModuleEffect::type).distinct()
                .forEach(type -> warnings.accept(root.path() + ": the " + type
                        + " effect needs the SkysongNPCs plugin, which is not installed, so it does nothing"));
        return new Module(id, root.requireString("name"), root.string("icon", DEFAULT_ICON), description(root), maxPicks,
                requirements(root), effects);
    }

    private static List<String> description(ConfigSection root) {
        Object raw = root.raw("description");
        return raw instanceof List<?> ? root.strings("description") : raw == null ? List.of() : List.of(raw.toString());
    }

    private static ModuleRequirements requirements(ConfigSection root) {
        if (!root.contains("requires")) {
            return ModuleRequirements.NONE;
        }
        ConfigSection requires = root.section("requires");
        int tier = requires.integer("tier", Business.MIN_TIER);
        if (tier < Business.MIN_TIER || tier > Business.MAX_TIER) {
            throw requires.error("tier", "must be " + Business.MIN_TIER + " to " + Business.MAX_TIER);
        }
        int level = requires.integer("level", 1);
        if (level < 1) {
            throw requires.error("level", "must be 1 or more");
        }
        return new ModuleRequirements(tier, level, requires.strings("modules").stream()
                .map(module -> module.strip().toLowerCase(Locale.ROOT)).toList());
    }
}
