package org.skysongdev.skysongEconomy.data.datatypes;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.items.SkysongItemFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

/**
 * An ImportOffer that generates items with random amounts within specified ranges.
 * Each item is defined by an ID and a min/max amount in the config.
 */
public class RandomRangeImportOffer extends ImportOffer{
    private static final Logger LOGGER = Logger.getLogger("SkysongEconomy");

    private final ArrayList<Integer> minAmounts = new ArrayList<>();
    private final ArrayList<Integer> maxAmounts = new ArrayList<>();

    public RandomRangeImportOffer(String offerName, ArrayList<String> configInputs, float priceModifier, String staticLore, boolean isAffectedByTaxes, Material iconMaterial, Map<Integer, Double> priceMap) {
        super(offerName, priceModifier, staticLore, isAffectedByTaxes, iconMaterial, new ArrayList<>(), new ArrayList<>(), priceMap);
        for(String input : configInputs) {
            String[] parts = input.split(";");
            if (parts.length != 3) {
                LOGGER.warning("Invalid item input format: " + input);
                continue;
            }
            itemIds.add(parts[0].trim());
            try {
                this.minAmounts.add(Integer.parseInt(parts[1].trim()));
                this.maxAmounts.add(Integer.parseInt(parts[2].trim()));
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid number format in item input: " + input);
                itemIds.remove(itemIds.size() - 1); // Remove the last added ID if parsing fails
            }
        }
    }

    @Override
    public ArrayList<ItemStack> generateItems(SkysongItemFactory itemFactory) {
        ArrayList<ItemStack> items = new ArrayList<>();
        for(int i = 0; i < itemIds.size(); i++) {
            int amount = (int) Math.floor(Math.random() * (maxAmounts.get(i) - minAmounts.get(i) + 1) + minAmounts.get(i));
            items.add(itemFactory.create(itemIds.get(i), amount));
        }
        return items;
    }
}
