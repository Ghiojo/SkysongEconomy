package org.skysongdev.skysongEconomy.business.module.effect;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

/** Reads one effect type from a module file. */
@FunctionalInterface
public interface ModuleEffectParser {
    /**
     * @param moduleId the module the effect belongs to
     * @param section  the effect's section, {@code type} included
     * @throws ConfigException when something in it is missing or wrong
     */
    ModuleEffect parse(String moduleId, ConfigSection section);
}
