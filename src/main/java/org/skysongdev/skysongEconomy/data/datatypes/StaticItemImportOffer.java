package org.skysongdev.skysongEconomy.data.datatypes;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.utils.Constants;
import org.skysongdev.skysongitems.SkysongItems;
import org.skysongdev.skysongitems.utils.SkysongItem;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.skysongdev.skysongEconomy.SkysongEconomy.Instance;

/*
 * This class represents an import offer that provides a fixed set of items based on their IDs and quantities.
 * The items are defined in the configuration as "itemId;quantity" pairs. When generating the offer, it loads
 * the corresponding SkysongItem for each item ID and creates ItemStacks with the specified quantities.
 */
public class StaticItemImportOffer extends ImportOffer {
    ArrayList<String> itemIds;
    ArrayList<Integer> itemAmounts;

    public StaticItemImportOffer(String offerName, ArrayList<String> configInputs, float priceModifier, String staticLore, boolean isAffectedByTaxes, Material iconMaterial, Map<Integer, Double> priceMap) {
        super(offerName, priceModifier, staticLore, isAffectedByTaxes, iconMaterial, new ArrayList<>(), new ArrayList<>(), priceMap);
        for(String input : configInputs) {
            String[] parts = input.split(";");
            if (parts.length != 2) {
                continue;
            }
            itemIds.add(parts[0].trim());
            try {
                itemAmounts.add(Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException e) {
                itemIds.remove(itemIds.size() - 1); // Remove the last added ID if parsing fails
            }
        }
    }


    @Override
    public List<String> getTags() throws SQLException {
        List<String> tags = new ArrayList<>();
        for(String itemId : itemIds) {
            tags.addAll(Instance().getDatabase().findAllGoodTypesFromMaterial(itemId));
        }
        return tags;
    }

    @Override
    public ArrayList<ItemStack> generateItems() {
        ArrayList<ItemStack> finalItems = new ArrayList<>();
        for(int i = 0; i < itemIds.size(); i++) {
            File itemFile = Constants.ITEM_GENERATION_FILE_PATH.resolve(itemIds.get(i) + ".yml").toFile();
            SkysongItem skysongItem = org.skysongdev.skysongitems.utils.Utils.LoadSkysongItem(itemFile);
            ItemStack item = SkysongItems.getPlugin().getUtils().GenerateSkysongItem(skysongItem);
            item.setAmount(itemAmounts.get(i));
            finalItems.add(item);
        }
        return finalItems;
    }
}
