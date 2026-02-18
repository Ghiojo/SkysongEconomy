package org.skysongdev.skysongEconomy.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongEconomy.data.datatypes.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class Utils {
    private static Utils instance;
    public static Map<String, EconomyBusiness> economyBusinesses;
    public static Map<String, ImportCatalogue> importCatalogues;

    public static Utils Instance(){
        if(instance == null){
            instance = new Utils();
        }
        return instance;
    }

    public static void initializeImportCatalogues(){
        for(File file : Constants.IMPORT_OFFER_PATH.toFile().listFiles()){
            importCatalogues = new java.util.HashMap<>();
            File root = Constants.IMPORT_OFFER_PATH.toFile();
            if (root == null || !root.exists()) return;
            File[] children = root.listFiles();
            if (children == null) return;
            for (File folder : children) {
                if (!folder.isDirectory()) continue;
                String key = folder.getName();
                java.util.List<ImportOffer> offers = new java.util.ArrayList<>();
                for(File offerFile : folder.listFiles()){
                    if(offerFile.isFile() && offerFile.getName().endsWith(".yml")){
                        ImportOffer offer = Instance().parseImportOffer(offerFile);
                        if(offer != null){
                            offers.add(offer);
                        }
                    }
                }
                ImportCatalogue catalogue = new ImportCatalogue(offers);
                importCatalogues.put(key, catalogue);
            }

        }
    }

    public ImportOffer parseImportOffer(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String offerName = yaml.getString("offerName");
        String staticLore = yaml.getString("staticLore");
        ArrayList<String> configInputs = new ArrayList<>(yaml.getStringList("inputs"));
        boolean isAffectedByTaxes = yaml.getBoolean("isAffectedByTaxes");
        Material iconMaterial = Material.getMaterial(yaml.getString("iconMaterial"));
        ArrayList<String> pricesAsString = new ArrayList<>(yaml.getStringList("priceMap"));
        Map<Integer, Double> priceMap = new java.util.HashMap<>();
        for (String entry : pricesAsString) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                try {
                    int quantity = Integer.parseInt(parts[0].trim());
                    double price = Double.parseDouble(parts[1].trim());
                    priceMap.put(quantity, price);
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("[SkysongEconomy] Invalid price map entry in " + file.getName() + ": " + entry);
                }
            }
        }

        switch (yaml.getString("offerType").toUpperCase()) {
            case "STATIC":
                return new StaticItemImportOffer(offerName, configInputs, 1, staticLore, isAffectedByTaxes, iconMaterial, priceMap);
            case "RANDOM_RANGE":
                return new RandomRangeImportOffer(offerName, configInputs, 1, staticLore, isAffectedByTaxes, iconMaterial, priceMap);
            case "RANDOM_ITEM":
                return new RandomItemImportOffer(offerName, configInputs, yaml.getInt("lower_end"), yaml.getInt("upper_end"), 1, staticLore, isAffectedByTaxes, iconMaterial, priceMap);
            default:
                Bukkit.getLogger().warning("[SkysongEconomy] Invalid offer type in " + file.getName() + ": " + yaml.getString("offerType"));
                return null;
        }
    }
}
