package org.skysongdev.skysongEconomy.items;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongitems.api.ItemTemplates;

import java.nio.file.Path;

/**
 * Builds Skysong items from the {@code <materialId>.yml} template files in one folder, through
 * the public SkysongItems API, so it keeps working when the plugin's internal classes change.
 */
public final class FileSkysongItemFactory implements SkysongItemFactory {
    private final ItemTemplates templates;
    private final Path folder;

    /**
     * @param templates the SkysongItems template API, from {@code SkysongItemsApi.get().templates()}
     * @param folder    the folder holding one {@code <materialId>.yml} template per item
     */
    public FileSkysongItemFactory(ItemTemplates templates, Path folder) {
        this.templates = templates;
        this.folder = folder;
    }

    @Override
    public boolean exists(String materialId) {
        return folder.resolve(materialId + ".yml").toFile().isFile();
    }

    @Override
    public ItemStack create(String materialId, int amount) {
        if (!exists(materialId)) {
            throw new IllegalArgumentException("No Skysong item file for '" + materialId + "' in " + folder);
        }
        ItemStack item = templates.create(folder.resolve(materialId + ".yml"));
        item.setAmount(amount);
        return item;
    }
}
