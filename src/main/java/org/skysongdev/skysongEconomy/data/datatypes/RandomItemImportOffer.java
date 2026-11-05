package org.skysongdev.skysongEconomy.data.datatypes;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.items.SkysongItemFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

public class RandomItemImportOffer extends ImportOffer {
    private static final Logger LOGGER = Logger.getLogger("SkysongEconomy");

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
                LOGGER.warning("Invalid item input format: " + input);
                continue;
            }
            itemIds.add(parts[0].trim());
            try {
                itemChances.add(Float.parseFloat(parts[1].trim()));
                itemQuantities.add(Integer.parseInt(parts[2].trim()));
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid number format in item input: " + input);
                itemIds.remove(itemIds.size() - 1); // Remove the last added ID if parsing fails
            }
        }
    }

    @Override
    public ArrayList<ItemStack> generateItems(SkysongItemFactory items) {
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
                    finalItems.add(items.create(itemIds.get(j), itemQuantities.get(j)));
                    break;
                }
            }
        }

        return finalItems;
    }
}
