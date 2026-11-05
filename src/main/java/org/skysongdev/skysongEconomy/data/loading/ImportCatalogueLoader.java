package org.skysongdev.skysongEconomy.data.loading;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongEconomy.data.datatypes.ImportCatalogue;
import org.skysongdev.skysongEconomy.data.datatypes.ImportOffer;
import org.skysongdev.skysongEconomy.data.datatypes.RandomItemImportOffer;
import org.skysongdev.skysongEconomy.data.datatypes.RandomRangeImportOffer;
import org.skysongdev.skysongEconomy.data.datatypes.StaticItemImportOffer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Reads import catalogues from {@code imports/<catalogue>/<offer>.yml}. Each sub-folder is one
 * catalogue, named after the folder. Invalid offers are logged and skipped.
 */
public final class ImportCatalogueLoader {
    private final Path importsFolder;
    private final Logger logger;

    public ImportCatalogueLoader(Path importsFolder, Logger logger) {
        this.importsFolder = importsFolder;
        this.logger = logger;
    }

    /** @return catalogues by name; empty if the imports folder does not exist */
    public Map<String, ImportCatalogue> loadAll() {
        Map<String, ImportCatalogue> catalogues = new HashMap<>();
        File[] catalogueFolders = importsFolder.toFile().listFiles(File::isDirectory);
        if (catalogueFolders == null) {
            return catalogues;
        }
        for (File folder : catalogueFolders) {
            catalogues.put(folder.getName(), new ImportCatalogue(loadOffers(folder)));
        }
        return catalogues;
    }

    private List<ImportOffer> loadOffers(File catalogueFolder) {
        List<ImportOffer> offers = new ArrayList<>();
        File[] offerFiles = catalogueFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (offerFiles == null) {
            return offers;
        }
        for (File offerFile : offerFiles) {
            parseOffer(offerFile).ifPresent(offers::add);
        }
        return offers;
    }

    Optional<ImportOffer> parseOffer(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String offerName = yaml.getString("offerName");
        String staticLore = yaml.getString("staticLore");
        ArrayList<String> configInputs = new ArrayList<>(yaml.getStringList("inputs"));
        boolean isAffectedByTaxes = yaml.getBoolean("isAffectedByTaxes");
        Material iconMaterial = parseIcon(file, yaml.getString("iconMaterial", "PAPER"));
        Map<Integer, Double> priceMap = parsePriceMap(file, yaml.getStringList("priceMap"));
        float priceModifier = 1;

        String offerType = yaml.getString("offerType", "");
        return switch (offerType.toUpperCase(Locale.ROOT)) {
            case "STATIC" -> Optional.of(new StaticItemImportOffer(
                    offerName, configInputs, priceModifier, staticLore, isAffectedByTaxes, iconMaterial, priceMap));
            case "RANDOM_RANGE" -> Optional.of(new RandomRangeImportOffer(
                    offerName, configInputs, priceModifier, staticLore, isAffectedByTaxes, iconMaterial, priceMap));
            case "RANDOM_ITEM" -> Optional.of(new RandomItemImportOffer(
                    offerName, configInputs, yaml.getInt("lower_end"), yaml.getInt("upper_end"),
                    priceModifier, staticLore, isAffectedByTaxes, iconMaterial, priceMap));
            default -> {
                logger.warning("Invalid offerType '" + offerType + "' in " + file + ", skipping it");
                yield Optional.empty();
            }
        };
    }

    private Material parseIcon(File file, String name) {
        Material material = Material.matchMaterial(name);
        if (material == null) {
            logger.warning("Invalid iconMaterial '" + name + "' in " + file + ", using PAPER");
            return Material.PAPER;
        }
        return material;
    }

    /** Entries look like {@code "<int>:<price>"}. */
    private Map<Integer, Double> parsePriceMap(File file, List<String> entries) {
        Map<Integer, Double> priceMap = new HashMap<>();
        for (String entry : entries) {
            String[] parts = entry.split(":");
            try {
                if (parts.length != 2) {
                    throw new NumberFormatException();
                }
                priceMap.put(Integer.parseInt(parts[0].trim()), Double.parseDouble(parts[1].trim()));
            } catch (NumberFormatException e) {
                logger.warning("Invalid priceMap entry '" + entry + "' in " + file);
            }
        }
        return priceMap;
    }
}
