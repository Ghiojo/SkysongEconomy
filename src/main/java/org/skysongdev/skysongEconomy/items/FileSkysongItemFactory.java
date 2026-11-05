package org.skysongdev.skysongEconomy.items;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongitems.SkysongItems;
import org.skysongdev.skysongitems.utils.SkysongItem;
import org.skysongdev.skysongitems.utils.Utils;

import java.io.File;
import java.nio.file.Path;

/** Builds Skysong items from the {@code <materialId>.yml} files in one folder, using the Skysong_Items plugin. */
public final class FileSkysongItemFactory implements SkysongItemFactory {
    private final Path folder;

    public FileSkysongItemFactory(Path folder) {
        this.folder = folder;
    }

    @Override
    public ItemStack create(String materialId, int amount) {
        File file = folder.resolve(materialId + ".yml").toFile();
        if (!file.isFile()) {
            throw new IllegalArgumentException("No Skysong item file for '" + materialId + "' in " + folder);
        }
        SkysongItem skysongItem = Utils.LoadSkysongItem(file);
        ItemStack item = SkysongItems.getPlugin().getUtils().GenerateSkysongItem(skysongItem);
        item.setAmount(amount);
        return item;
    }
}
