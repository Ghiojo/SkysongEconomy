package org.skysongdev.skysongEconomy.business.module;

import org.skysongdev.skysongEconomy.business.module.effect.ModuleEffect;
import org.skysongdev.skysongEconomy.config.FileEntry;

import java.util.List;

/**
 * One premade module, from {@code modules/<id>.yml}.
 *
 * @param id          the file name without {@code .yml}
 * @param name        its name, as MiniMessage
 * @param icon        the Minecraft item that stands for it in menus
 * @param description what it means for the business, as MiniMessage lines
 * @param maxPicks    how many times one business may pick it, at least 1
 * @param requires    what a business needs before it can pick it
 * @param effects     what it does once picked, all together; none for a module that is its description only
 */
public record Module(String id, String name, String icon, List<String> description, int maxPicks,
                     ModuleRequirements requires, List<ModuleEffect> effects) implements FileEntry {
    public Module {
        description = List.copyOf(description);
        effects = List.copyOf(effects);
        if (maxPicks < 1) {
            throw new IllegalArgumentException("maxPicks must be at least 1");
        }
    }

    /** @return the module's effects of one type, in file order */
    public <T extends ModuleEffect> List<T> effects(Class<T> type) {
        return effects.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
