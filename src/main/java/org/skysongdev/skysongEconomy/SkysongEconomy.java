package org.skysongdev.skysongEconomy;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.business.type.BusinessTypesParser;
import org.skysongdev.skysongEconomy.commands.SkysongEconomyCommand;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.PluginPaths;
import org.skysongdev.skysongEconomy.config.ResourceExtractor;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.currency.ArchititeCurrency;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSchema;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.EconomyBusinessRepository;
import org.skysongdev.skysongEconomy.data.database.GoodTypeRepository;
import org.skysongdev.skysongEconomy.data.database.PriceLedgerRepository;
import org.skysongdev.skysongEconomy.data.datatypes.ImportCatalogue;
import org.skysongdev.skysongEconomy.data.loading.ImportCatalogueLoader;
import org.skysongdev.skysongEconomy.integration.skysongstats.SkysongStatsAdapter;
import org.skysongdev.skysongEconomy.items.FileSkysongItemFactory;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.items.SkysongItemFactory;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongitems.SkysongItems;
import org.skysongdev.skysongstats.api.SkysongStatsApi;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Plugin entry point. It only creates the plugin's services, connects them to each other and
 * registers them with Bukkit; the features live in their own classes.
 */
public final class SkysongEconomy extends JavaPlugin {
    private ConnectionProvider connections;
    private PriceLedgerRepository priceLedger;
    private GoodTypeRepository goodTypes;
    private EconomyBusinessRepository economyBusinesses;
    private SkysongItemFactory importItems;
    private Map<String, ImportCatalogue> importCatalogues = Map.of();
    private SkysongStatsAdapter skysongStats;
    private BusinessTypeRegistry businessTypes = new BusinessTypeRegistry(List.of());

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PluginPaths paths = new PluginPaths(getDataFolder().toPath());
        new ResourceExtractor(this, getFile()).extractFolder(PluginPaths.COINS_FOLDER);

        setUpDatabase();
        new ArchititeCurrency(new FileSkysongItemFactory(paths.coins()), getLogger()).registerDenominations();

        importItems = new FileSkysongItemFactory(paths.items());
        importCatalogues = new ImportCatalogueLoader(paths.imports(), getLogger()).loadAll();
        getLogger().info("Loaded " + importCatalogues.size() + " import catalogue(s)");

        skysongStats = new SkysongStatsAdapter(SkysongStatsApi.get());
        YamlFiles configFiles = new YamlFiles(getDataFolder().toPath(), path -> saveResource(path, false));
        loadBusinessTypes(configFiles);

        registerCommands();
    }

    @Override
    public void onDisable() {
        if (connections != null) {
            connections.close();
        }
    }

    private void setUpDatabase() {
        connections = new ConnectionProvider(DatabaseSettings.fromConfig(getConfig().getConfigurationSection("database")), getLogger());
        priceLedger = new PriceLedgerRepository(connections);
        goodTypes = new GoodTypeRepository(connections);
        economyBusinesses = new EconomyBusinessRepository(connections);
        try {
            DatabaseSchema.createTables(connections);
        } catch (SQLException e) {
            // Keep running: repositories reconnect on their next query once the database is reachable.
            getLogger().log(Level.SEVERE, "Unable to connect to the database and create tables!", e);
        }
    }

    private void loadBusinessTypes(YamlFiles configFiles) {
        try {
            BusinessTypesParser parser = new BusinessTypesParser(skysongStats, getLogger()::warning);
            businessTypes = parser.parse(configFiles.load(BusinessTypesParser.FILE));
            getLogger().info("Loaded " + businessTypes.all().size() + " business type(s)");
        } catch (ConfigException e) {
            // Businesses can't work without types, but the rest of the plugin still can.
            getLogger().severe("Business types were not loaded: " + e.getMessage());
        }
    }

    private void registerCommands() {
        Messenger messenger = new Messenger(MiniMessage.miniMessage());
        MaterialIdResolver materialIds = new MaterialIdResolver(new NamespacedKey(SkysongItems.getPlugin(), "material_id"));
        PluginCommand command = getCommand("se");
        if (command == null) {
            throw new IllegalStateException("The 'se' command is missing from plugin.yml");
        }
        command.setExecutor(new SkysongEconomyCommand(priceLedger, goodTypes, materialIds, messenger, getLogger()));
    }

    public PriceLedgerRepository getPriceLedger() {
        return priceLedger;
    }

    public GoodTypeRepository getGoodTypes() {
        return goodTypes;
    }

    public EconomyBusinessRepository getEconomyBusinesses() {
        return economyBusinesses;
    }

    /** Builds the items that import offers deliver, from the {@code items} folder. */
    public SkysongItemFactory getImportItems() {
        return importItems;
    }

    public Map<String, ImportCatalogue> getImportCatalogues() {
        return importCatalogues;
    }

    /** Characters, skills, proficiencies and downtime, from SkysongStats. */
    public SkysongStatsAdapter getSkysongStats() {
        return skysongStats;
    }

    /** The business types defined in {@code businesses/types.yml}. */
    public BusinessTypeRegistry getBusinessTypes() {
        return businessTypes;
    }
}
