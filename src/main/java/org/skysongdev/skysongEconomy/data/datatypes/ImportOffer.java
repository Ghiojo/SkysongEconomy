package org.skysongdev.skysongEconomy.data.datatypes;


import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ImportOffer {
    private String offerName;
    private float priceModifier;
    private String staticLore;
    private Material iconMaterial;
    protected final ArrayList<String> itemIds;
    protected final ArrayList<Integer> itemQuantities;
    private boolean isAffectedByTaxes;
    private Map<Integer, Double> priceMap;

    public ImportOffer(String offerName, float priceModifier, String staticLore, boolean isAffectedByTaxes, Material iconMaterial, ArrayList<String> itemIds, ArrayList<Integer> itemQuantities, Map<Integer, Double> priceMap) {
        this.offerName = offerName;
        this.priceModifier = priceModifier;
        this.staticLore = staticLore;
        this.isAffectedByTaxes = isAffectedByTaxes;
        this.iconMaterial = iconMaterial;
        this.itemIds = itemIds;
        this.itemQuantities = itemQuantities;
        this.priceMap = priceMap;
    }

    public String getOfferName() {
        return offerName;
    }
    public float getPriceModifier() {
        return priceModifier;
    }
    public String getStaticLore() {
        return staticLore;
    }
    public boolean isAffectedByTaxes() {
        return isAffectedByTaxes;
    }

    /** Material IDs of every item this offer can deliver; look up their good types with GoodTypeRepository. */
    public List<String> getItemIds() {
        return List.copyOf(itemIds);
    }

    public abstract ArrayList<ItemStack> generateItems();
}
