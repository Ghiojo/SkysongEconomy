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
import java.util.Map;

public class RandomItemImportOffer extends ImportOffer {
    private final ArrayList<Float> itemChances;
    private final int lowerBoundOfItems;
    private final int upperBoundOfItems;


    public RandomItemImportOffer(String offerName, ArrayList<String> configInputs, int lowerBoundOfItems, int upperBoundOfItems, float priceModifier, String staticLore, boolean isAffectedByTaxes, Material iconMaterial, Map<Integer, Double> priceMap) {
        super(offerName, priceModifier, staticLore, isAffectedByTaxes, iconMaterial, new ArrayList<>(), new ArrayList<>(), priceMap);
        this.itemChances = new ArrayList<>();
        this.lowerBoundOfItems = lowerBoundOfItems;
        this.upperBoundOfItems = upperBoundOfItems;

        for (String input : configInputs) {
            String[] parts = input.split(";");
            if (parts.length != 3) {
                SkysongItems.getPlugin().getLogger().warning("Invalid item input format: " + input);
                continue;
            }
            itemIds.add(parts[0].trim());
            try {
                itemChances.add(Float.parseFloat(parts[1].trim()));
                itemQuantities.add(Integer.parseInt(parts[2].trim()));
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
        int rolls = (int) Math.floor(Math.random() * (upperBoundOfItems - lowerBoundOfItems + 1) + lowerBoundOfItems);
        ArrayList<ItemStack> finalItems = new ArrayList<>();
        // Normalize chances so they sum to 1.0
        float total = 0f;
        for (float c : itemChances) total += c;
        if (total <= 0) return finalItems;

        for (int i = 0; i < rolls; i++) {
            float roll = (float) Math.random();
            float cumulativeChance = 0f;

            for (int j = 0; j < itemChances.size(); j++) {
                cumulativeChance += itemChances.get(j) / total; // normalize
                if (roll <= cumulativeChance) {
                    File itemFile = Constants.ITEM_GENERATION_FILE_PATH.resolve(itemIds.get(j) + ".yml").toFile();
                    SkysongItem skysongItem = SkysongItems.getPlugin().getUtils().LoadSkysongItem(itemFile);
                    ItemStack item = SkysongItems.getPlugin().getUtils().GenerateSkysongItem(skysongItem);
                    item.setAmount(itemQuantities.get(j));
                    finalItems.add(item);
                    break;
                }
            }
        }

        return finalItems;
    }
}
