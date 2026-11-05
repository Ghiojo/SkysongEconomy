package org.skysongdev.skysongEconomy.data.datatypes;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.items.SkysongItemFactory;

import java.util.ArrayList;
import java.util.Map;

/*
 * This class represents an import offer that provides a fixed set of items based on their IDs and quantities.
 * The items are defined in the configuration as "itemId;quantity" pairs. When generating the offer, it loads
 * the corresponding SkysongItem for each item ID and creates ItemStacks with the specified quantities.
 */
public class StaticItemImportOffer extends ImportOffer {

    public StaticItemImportOffer(String offerName, ArrayList<String> configInputs, float priceModifier, String staticLore, boolean isAffectedByTaxes, Material iconMaterial, Map<Integer, Double> priceMap) {
        super(offerName, priceModifier, staticLore, isAffectedByTaxes, iconMaterial, new ArrayList<>(), new ArrayList<>(), priceMap);
        for(String input : configInputs) {
            String[] parts = input.split(";");
            if (parts.length != 2) {
                continue;
            }
            itemIds.add(parts[0].trim());
            try {
                itemQuantities.add(Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException e) {
                itemIds.remove(itemIds.size() - 1); // Remove the last added ID if parsing fails
            }
        }
    }


    @Override
    public ArrayList<ItemStack> generateItems(SkysongItemFactory items) {
        ArrayList<ItemStack> finalItems = new ArrayList<>();
        for(int i = 0; i < itemIds.size(); i++) {
            finalItems.add(items.create(itemIds.get(i), itemQuantities.get(i)));
        }
        return finalItems;
    }
}
