package org.skysongdev.skysongEconomy.data.datatypes;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.SkysongEconomy;
import org.skysongdev.skysongEconomy.utils.Constants;
import org.skysongdev.skysongitems.SkysongItems;
import org.skysongdev.skysongitems.utils.SkysongItem;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * An ImportOffer that generates items with random amounts within specified ranges.
 * Each item is defined by an ID and a min/max amount in the config.
 */
public class RandomRangeImportOffer extends ImportOffer{
    private ArrayList<Integer> minAmounts;
    private ArrayList<Integer> maxAmounts;

    public RandomRangeImportOffer(String offerName, ArrayList<String> configInputs, float priceModifier, String staticLore, boolean isAffectedByTaxes, Material iconMaterial) {
        super(offerName, priceModifier, staticLore, isAffectedByTaxes, iconMaterial, new ArrayList<>(), new ArrayList<>());
        for(String input : configInputs) {
            String[] parts = input.split(";");
            if (parts.length != 3) {
                SkysongItems.getPlugin().getLogger().warning("Invalid item input format: " + input);
                continue;
            }
            itemIds.add(parts[0].trim());
            try {
                this.minAmounts.add(Integer.parseInt(parts[1].trim()));
                this.maxAmounts.add(Integer.parseInt(parts[2].trim()));
            } catch (NumberFormatException e) {
                SkysongItems.getPlugin().getLogger().warning("Invalid number format in item input: " + input);
                itemIds.remove(itemIds.size() - 1); // Remove the last added ID if parsing fails
            }
        }
    }

    @Override
    public List<String> getTags() throws SQLException {
        List<String> tags = new ArrayList<>();
        for (String itemId : itemIds) {
            tags.addAll(SkysongEconomy.Instance().getDatabase().findAllGoodTypesFromMaterial(itemId));
        }
        return tags;
    }

    @Override
    public ArrayList<ItemStack> generateItems() {
        ArrayList<ItemStack> items = new ArrayList<>();
        for(int i = 0; i < itemIds.size(); i++) {
            int amount = (int) Math.floor(Math.random() * (maxAmounts.get(i) - minAmounts.get(i) + 1) + minAmounts.get(i));
            File itemFile = Constants.ITEM_GENERATION_FILE_PATH.resolve(itemIds.get(i) + ".yml").toFile();
            SkysongItem skysongItem = org.skysongdev.skysongitems.utils.Utils.LoadSkysongItem(itemFile);
            ItemStack item = SkysongItems.getPlugin().getUtils().GenerateSkysongItem(skysongItem);
            item.setAmount(amount);
            items.add(item);
        }
        return items;
    }
}
