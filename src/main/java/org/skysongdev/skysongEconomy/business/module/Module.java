package org.skysongdev.skysongEconomy.business.module;

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
 */
public record Module(String id, String name, String icon, List<String> description, int maxPicks,
                     ModuleRequirements requires) implements FileEntry {
    public Module {
        description = List.copyOf(description);
        if (maxPicks < 1) {
            throw new IllegalArgumentException("maxPicks must be at least 1");
        }
    }
}
