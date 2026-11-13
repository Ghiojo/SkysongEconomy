package org.skysongdev.skysongEconomy.business.module;

import java.util.List;

/**
 * What a business needs before it can pick a module: {@code requires} in the module's file.
 *
 * @param tier    the business tier it needs at least; 1 asks for nothing
 * @param level   how many picks of the skill the module is picked for it needs at least; 1 asks for nothing
 * @param modules modules the business must already have, by id
 */
public record ModuleRequirements(int tier, int level, List<String> modules) {
    /** Asks for nothing. */
    public static final ModuleRequirements NONE = new ModuleRequirements(1, 1, List.of());

    public ModuleRequirements {
        modules = List.copyOf(modules);
    }
}
