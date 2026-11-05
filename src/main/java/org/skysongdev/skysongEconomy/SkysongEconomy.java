package org.skysongdev.skysongEconomy;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongEconomy.bootstrap.BusinessFeature;
import org.skysongdev.skysongEconomy.commands.SkysongEconomyCommand;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
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
import org.skysongdev.skysongEconomy.integration.gringotts.GringottsWallet;
import org.skysongdev.skysongEconomy.integration.skysongstats.SkysongStatsAdapter;
import org.skysongdev.skysongEconomy.items.FileSkysongItemFactory;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.items.SkysongItemFactory;
import org.skysongdev.skysongEconomy.messaging.MessageCatalog;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongitems.SkysongItems;
import org.skysongdev.skysongstats.api.SkysongStatsApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
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
    private BusinessFeature businessFeature;

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
        MessageCatalog messages = loadMessages(configFiles);
        Messenger messenger = new Messenger(MiniMessage.miniMessage(), messages);
        businessFeature = BusinessFeature.start(this, configFiles, databaseSettings(), skysongStats,
                new GringottsWallet(), messenger, messages);

        registerCommands(messenger);
    }

    @Override
    public void onDisable() {
        if (businessFeature != null) {
            businessFeature.close();
        }
        if (connections != null) {
            connections.close();
        }
    }

    private void setUpDatabase() {
        connections = new ConnectionProvider(databaseSettings(), getLogger());
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

    private DatabaseSettings databaseSettings() {
        return DatabaseSettings.fromConfig(getConfig().getConfigurationSection("database"));
    }

    /** Reads messages.yml over the defaults bundled in the jar, so new keys always have a text. */
    private MessageCatalog loadMessages(YamlFiles configFiles) {
        ConfigSection defaults;
        try (InputStream bundled = getResource(MessageCatalog.FILE);
             Reader reader = new InputStreamReader(Objects.requireNonNull(bundled, "bundled messages.yml"), StandardCharsets.UTF_8)) {
            defaults = YamlFiles.parse(MessageCatalog.FILE, reader);
        } catch (IOException e) {
            throw new IllegalStateException("The bundled messages.yml can't be read", e);
        }
        try {
            return MessageCatalog.of(defaults, configFiles.load(MessageCatalog.FILE), getLogger()::warning);
        } catch (ConfigException e) {
            getLogger().severe("Using the default messages: " + e.getMessage());
            return MessageCatalog.of(defaults, ConfigSection.root(MessageCatalog.FILE, null), getLogger()::warning);
        }
    }

    private void registerCommands(Messenger messenger) {
        MaterialIdResolver materialIds = new MaterialIdResolver(new NamespacedKey(SkysongItems.getPlugin(), "material_id"));
        PluginCommand command = getCommand("se");
        if (command == null) {
            throw new IllegalStateException("The 'se' command is missing from plugin.yml");
        }
        SkysongEconomyCommand executor = new SkysongEconomyCommand(priceLedger, goodTypes, materialIds, messenger, getLogger());
        businessFeature.commands().forEach(executor::register);
        command.setExecutor(executor);
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

    /** The business system; its parts are empty when its config couldn't be read. */
    public BusinessFeature getBusinessFeature() {
        return businessFeature;
    }
}
