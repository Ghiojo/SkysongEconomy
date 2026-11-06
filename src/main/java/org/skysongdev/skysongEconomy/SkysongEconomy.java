package org.skysongdev.skysongEconomy;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongEconomy.bootstrap.BusinessFeature;
import org.skysongdev.skysongEconomy.bootstrap.NodeFeature;
import org.skysongdev.skysongEconomy.bootstrap.TradeInputs;
import org.skysongdev.skysongEconomy.commands.SkysongEconomyCommand;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.PluginPaths;
import org.skysongdev.skysongEconomy.config.ResourceExtractor;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.currency.ArchititeCurrency;
import org.skysongdev.skysongEconomy.data.database.CachedGoodTypes;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSchema;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.GoodTypeRepository;
import org.skysongdev.skysongEconomy.data.database.PriceLedgerRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.integration.gringotts.GringottsWallet;
import org.skysongdev.skysongEconomy.integration.skysongstats.SkysongStatsAdapter;
import org.skysongdev.skysongEconomy.items.FileSkysongItemFactory;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.items.SkysongItemFactory;
import org.skysongdev.skysongEconomy.messaging.MessageCatalog;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.prices.PriceDatabase;
import org.skysongdev.skysongEconomy.prices.PriceSettings;
import org.skysongdev.skysongitems.api.SkysongItemsApi;
import org.skysongdev.skysongstats.api.SkysongStatsApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Plugin entry point. It only creates the plugin's services, connects them to each other and
 * registers them with Bukkit; the features live in their own classes.
 */
public final class SkysongEconomy extends JavaPlugin {
    private ConnectionProvider connections;
    private PriceLedgerRepository priceLedger;
    private PriceDatabase prices;
    private PriceSettings priceSettings;
    private CachedGoodTypes goodTypes;
    private SkysongItemFactory importItems;
    private SkysongStatsAdapter skysongStats;
    private BusinessFeature businessFeature;
    private NodeFeature nodeFeature;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PluginPaths paths = new PluginPaths(getDataFolder().toPath());
        new ResourceExtractor(this, getFile()).extractFolder(PluginPaths.COINS_FOLDER);

        setUpDatabase();
        SkysongItemsApi skysongItems = SkysongItemsApi.get();
        new ArchititeCurrency(new FileSkysongItemFactory(skysongItems.templates(), paths.coins()), getLogger())
                .registerDenominations();

        importItems = new FileSkysongItemFactory(skysongItems.templates(), paths.items());

        skysongStats = new SkysongStatsAdapter(SkysongStatsApi.get());
        YamlFiles configFiles = new YamlFiles(getDataFolder().toPath(), path -> saveResource(path, false));
        MessageCatalog messages = loadMessages(configFiles);
        Messenger messenger = new Messenger(MiniMessage.miniMessage(), messages);
        loadPrices(configFiles);
        MaterialIdResolver materialIds = new MaterialIdResolver(skysongItems.tags());
        Wallet wallet = new GringottsWallet();
        TradeInputs trade = new TradeInputs(importItems, prices, priceSettings, goodTypes, materialIds);
        businessFeature = BusinessFeature.start(this, paths, configFiles, databaseSettings(), skysongStats, wallet, trade,
                messenger, messages);
        nodeFeature = NodeFeature.start(this, configFiles, databaseSettings(), businessFeature.system(), skysongStats,
                wallet, trade, messenger);

        registerCommands(messenger, materialIds);
    }

    @Override
    public void onDisable() {
        // Nodes listen to businesses, so they close first.
        if (nodeFeature != null) {
            nodeFeature.close();
        }
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
        goodTypes = new CachedGoodTypes(new GoodTypeRepository(connections));
        try {
            DatabaseSchema.createTables(connections);
            goodTypes.loadAll();
        } catch (SQLException e) {
            // Keep running: repositories reconnect on their next query once the database is reachable.
            getLogger().log(Level.SEVERE, "Unable to connect to the database and create tables!", e);
        }
    }

    /** Loads prices.yml and the price database; a broken prices.yml falls back to 1 decimal. */
    private void loadPrices(YamlFiles configFiles) {
        try {
            priceSettings = PriceSettings.parse(configFiles.load(PriceSettings.FILE));
        } catch (ConfigException e) {
            getLogger().severe("Prices keep 1 decimal until this is fixed: " + e.getMessage());
            priceSettings = new PriceSettings(1);
        }
        prices = new PriceDatabase(priceLedger);
        try {
            prices.loadAll(getLogger()::warning);
            getLogger().info("Loaded " + prices.all().size() + " base value(s) into the price database");
        } catch (StorageException e) {
            getLogger().log(Level.SEVERE, "The price database could not be loaded", e);
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

    private void registerCommands(Messenger messenger, MaterialIdResolver materialIds) {
        PluginCommand command = getCommand("se");
        if (command == null) {
            throw new IllegalStateException("The 'se' command is missing from plugin.yml");
        }
        SkysongEconomyCommand executor = new SkysongEconomyCommand(prices, priceSettings, goodTypes, materialIds, messenger,
                getLogger());
        businessFeature.commands().forEach(executor::register);
        nodeFeature.commands().forEach(executor::register);
        command.setExecutor(executor);
    }

    /** The price database: every item's base value. */
    public PriceDatabase getPrices() {
        return prices;
    }

    /** How many decimals prices keep. */
    public PriceSettings getPriceSettings() {
        return priceSettings;
    }

    /** Every item's good types, kept in memory. */
    public CachedGoodTypes getGoodTypes() {
        return goodTypes;
    }

    /** Builds the items that imports deliver, from the {@code items} folder. */
    public SkysongItemFactory getImportItems() {
        return importItems;
    }

    /** Characters, skills, proficiencies and downtime, from SkysongStats. */
    public SkysongStatsAdapter getSkysongStats() {
        return skysongStats;
    }

    /** The business system; its parts are empty when its config couldn't be read. */
    public BusinessFeature getBusinessFeature() {
        return businessFeature;
    }

    /** The nodes system; its parts are empty when its config couldn't be read. */
    public NodeFeature getNodeFeature() {
        return nodeFeature;
    }
}
