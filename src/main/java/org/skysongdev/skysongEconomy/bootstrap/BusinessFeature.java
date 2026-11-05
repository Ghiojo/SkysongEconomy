package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.access.AccessDefaults;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.activity.ActivityLock;
import org.skysongdev.skysongEconomy.business.display.NameplateSettings;
import org.skysongdev.skysongEconomy.business.display.NameplateSettingsParser;
import org.skysongdev.skysongEconomy.business.shift.ShiftService;
import org.skysongdev.skysongEconomy.business.shift.ShiftSettings;
import org.skysongdev.skysongEconomy.business.shift.ShiftSettingsParser;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.training.TrainingService;
import org.skysongdev.skysongEconomy.business.training.TrainingSettings;
import org.skysongdev.skysongEconomy.business.training.TrainingSettingsParser;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiersParser;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.business.type.BusinessTypesParser;
import org.skysongdev.skysongEconomy.business.vault.BusinessVaults;
import org.skysongdev.skysongEconomy.commands.CharacterArguments;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.commands.business.BusinessCommand;
import org.skysongdev.skysongEconomy.commands.modifier.ModifierCommand;
import org.skysongdev.skysongEconomy.commands.tokens.TokensCommand;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.PluginPaths;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.data.database.AsyncAccessRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncBusinessRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncDeliveryRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncLedgerSelectionRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncModifierRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncShiftRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncTokenRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncTrainingRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncVaultRepository;
import org.skysongdev.skysongEconomy.data.database.BackgroundWriter;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.SqlAccessRepository;
import org.skysongdev.skysongEconomy.data.database.SqlBusinessRepository;
import org.skysongdev.skysongEconomy.data.database.SqlDeliveryRepository;
import org.skysongdev.skysongEconomy.data.database.SqlLedgerSelectionRepository;
import org.skysongdev.skysongEconomy.data.database.SqlModifierRepository;
import org.skysongdev.skysongEconomy.data.database.SqlShiftRepository;
import org.skysongdev.skysongEconomy.data.database.SqlTokenRepository;
import org.skysongdev.skysongEconomy.data.database.SqlTrainingRepository;
import org.skysongdev.skysongEconomy.data.database.SqlVaultRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.events.BukkitShiftEvents;
import org.skysongdev.skysongEconomy.gui.ChatPrompts;
import org.skysongdev.skysongEconomy.gui.MenuListener;
import org.skysongdev.skysongEconomy.gui.MenuTheme;
import org.skysongdev.skysongEconomy.gui.business.BusinessMenus;
import org.skysongdev.skysongEconomy.gui.business.ImportsEntry;
import org.skysongdev.skysongEconomy.gui.business.LedgersEntry;
import org.skysongdev.skysongEconomy.gui.business.RetrievePayEntry;
import org.skysongdev.skysongEconomy.gui.business.SkillPicksEntry;
import org.skysongdev.skysongEconomy.gui.business.TrainEntry;
import org.skysongdev.skysongEconomy.gui.business.VaultEntry;
import org.skysongdev.skysongEconomy.gui.business.WorkShiftEntry;
import org.skysongdev.skysongEconomy.gui.layout.MenuIcons;
import org.skysongdev.skysongEconomy.integration.cmi.AfkCheck;
import org.skysongdev.skysongEconomy.integration.cmi.CmiAfkCheck;
import org.skysongdev.skysongEconomy.integration.placeholderapi.TokenExpansion;
import org.skysongdev.skysongEconomy.integration.skysongstats.SkysongStatsAdapter;
import org.skysongdev.skysongEconomy.items.SkysongItemFactory;
import org.skysongdev.skysongEconomy.listeners.ActivityTimer;
import org.skysongdev.skysongEconomy.listeners.BusinessBlockListener;
import org.skysongdev.skysongEconomy.listeners.BusinessInteractListener;
import org.skysongdev.skysongEconomy.listeners.BusinessNameplates;
import org.skysongdev.skysongEconomy.messaging.MessageCatalog;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierTarget;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;
import org.skysongdev.skysongEconomy.tokens.TokenPlaceholders;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettingsParser;
import org.skysongdev.skysongEconomy.trade.LedgerLoader;
import org.skysongdev.skysongEconomy.trade.LedgerParser;
import org.skysongdev.skysongEconomy.trade.LedgerRegistry;
import org.skysongdev.skysongEconomy.trade.LedgerSelections;
import org.skysongdev.skysongEconomy.trade.TradePricing;
import org.skysongdev.skysongEconomy.trade.delivery.DeliveryService;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds the business system and connects it to the server: config, storage, commands, menus,
 * timers and listeners. If its config can't be read the system stays off and the rest of the
 * plugin runs.
 */
public final class BusinessFeature implements AutoCloseable {
    private final @Nullable BusinessSystem system;
    private final List<SubCommand> commands;
    /** Closed in reverse order on shutdown. */
    private final List<AutoCloseable> resources;
    private final Logger logger;

    private BusinessFeature(@Nullable BusinessSystem system, List<SubCommand> commands, List<AutoCloseable> resources,
                            Logger logger) {
        this.system = system;
        this.logger = logger;
        this.commands = List.copyOf(commands);
        this.resources = List.copyOf(resources);
    }

    /** The config every part of the business system needs. */
    private record Settings(BusinessTypeRegistry types, BusinessTiers tiers, AccessDefaults access, ShiftSettings shifts,
                            TrainingSettings training, EconomyLogisticsSettings economyLogistics) {
    }

    /**
     * Reads the business config, loads every business and registers the commands, menus and
     * listeners.
     *
     * @param database the database to keep businesses in; the feature opens its own connection
     * @param wallet   players' money, for pay and vaults
     * @param trade    what Economy and Logistics needs: items, prices and good types
     */
    public static BusinessFeature start(JavaPlugin plugin, PluginPaths paths, YamlFiles configFiles,
                                        DatabaseSettings database, SkysongStatsAdapter skysongStats, Wallet wallet,
                                        TradeInputs trade, Messenger messenger, MessageCatalog messages) {
        Logger logger = plugin.getLogger();
        Settings settings;
        try {
            settings = readSettings(configFiles, skysongStats, logger);
        } catch (ConfigException e) {
            logger.severe("Businesses are disabled until this is fixed: " + e.getMessage());
            return new BusinessFeature(null, List.of(), List.of(), logger);
        }

        List<AutoCloseable> resources = new ArrayList<>();
        ConnectionProvider connections = new ConnectionProvider(database, logger);
        resources.add(connections);
        // Every repository shares one writer thread, so rows are written in the order they changed.
        BackgroundWriter writer = new BackgroundWriter(logger);
        resources.add(writer);

        BusinessService businesses = new BusinessService(settings.types(), settings.tiers(),
                new AsyncBusinessRepository(new SqlBusinessRepository(connections), writer), Clock.systemUTC());
        AccessService access = new AccessService(settings.access(),
                new AsyncAccessRepository(new SqlAccessRepository(connections), writer));
        BusinessVaults vaults = new BusinessVaults(new AsyncVaultRepository(new SqlVaultRepository(connections), writer),
                wallet, logger::warning);
        GatheringTokens tokens = new GatheringTokens(new AsyncTokenRepository(new SqlTokenRepository(connections), writer));
        ActivityLock activities = new ActivityLock();
        PluginManager events = plugin.getServer().getPluginManager();
        ShiftService shifts = new ShiftService(settings.shifts(), new ShiftService.Dependencies(businesses,
                settings.tiers(), activities, vaults, tokens, wallet, skysongStats, new BukkitShiftEvents(events)),
                new AsyncShiftRepository(new SqlShiftRepository(connections), writer), Clock.systemUTC(), logger::warning);
        TrainingService training = new TrainingService(settings.training(), new TrainingService.Dependencies(businesses,
                settings.types(), settings.tiers(), access, activities, vaults, wallet, skysongStats, skysongStats),
                new AsyncTrainingRepository(new SqlTrainingRepository(connections), writer), Clock.systemUTC(),
                logger::warning);
        LedgerRegistry ledgers = loadLedgers(plugin, paths, settings.economyLogistics(), trade.items());
        LedgerSelections ledgerSelections = new LedgerSelections(settings.economyLogistics(), ledgers,
                new AsyncLedgerSelectionRepository(new SqlLedgerSelectionRepository(connections), writer), wallet,
                Clock.systemUTC());
        PriceModifiers modifiers = new PriceModifiers(
                new AsyncModifierRepository(new SqlModifierRepository(connections), writer), Clock.systemUTC());
        TradePricing pricing = new TradePricing(trade.prices(), modifiers, trade.priceSettings(), trade.goodTypes());
        DeliveryService deliveries = new DeliveryService(new DeliveryService.Dependencies(settings.economyLogistics(),
                ledgerSelections, access, pricing, wallet),
                new AsyncDeliveryRepository(new SqlDeliveryRepository(connections), writer), Clock.systemUTC(),
                new SecureRandom(), logger::warning);
        businesses.addListener(new BusinessChangeListener() {
            @Override
            public void deleted(Business business) {
                modifiers.removeAll(new ModifierTarget.Business(business.id()));
            }
        });
        // Shifts and training finish before the vault is emptied, so a deleted business pays everyone first.
        businesses.addListener(access);
        businesses.addListener(shifts);
        businesses.addListener(training);
        businesses.addListener(vaults);
        businesses.addListener(ledgerSelections);
        // Deliveries are refunded before the business's other rows go.
        businesses.addListener(deliveries);

        BusinessNameplates nameplates = new BusinessNameplates(businesses, nameplateSettings(configFiles, logger), messenger);
        businesses.addListener(nameplates);
        events.registerEvents(nameplates, plugin);
        resources.add(nameplates::close);
        try {
            logger.info("Loaded " + businesses.loadAll() + " business(es) of " + settings.types().all().size() + " type(s)");
            access.loadAll();
            vaults.loadAll();
            tokens.loadAll();
            shifts.loadAll();
            training.loadAll();
            ledgerSelections.loadAll();
            deliveries.loadAll();
            logger.info("Loaded " + modifiers.loadAll() + " price modifier(s)");
        } catch (StorageException e) {
            // Keep the commands so staff get clear errors, but nothing loaded means nothing to protect yet.
            logger.log(Level.SEVERE, "Businesses could not be loaded from the database", e);
        }

        events.registerEvents(new BusinessBlockListener(businesses, messenger), plugin);
        BusinessMenus menus = createMenus(plugin, configFiles, businesses, settings.types(), skysongStats, messenger, messages);
        menus.addEntry(new WorkShiftEntry(menus, shifts, activities, wallet, messenger));
        menus.addEntry(new RetrievePayEntry(menus, shifts, wallet, messenger));
        menus.addEntry(new TrainEntry(menus, training, activities, skysongStats, wallet, messenger));
        menus.addEntry(new VaultEntry(menus, vaults, messenger));
        menus.addEntry(new SkillPicksEntry(menus, businesses, skysongStats, messenger));
        menus.addEntry(new ImportsEntry(menus, ledgerSelections, deliveries, pricing, settings.economyLogistics(), wallet,
                messenger));
        menus.addEntry(new LedgersEntry(menus, ledgers, ledgerSelections, settings.economyLogistics(), wallet, messenger));
        events.registerEvents(new BusinessInteractListener(businesses, menus), plugin);

        resources.add(new ActivityTimer(plugin, businesses, shifts, training, skysongStats, afkCheck(plugin), messenger)
                .start()::cancel);
        registerPlaceholders(plugin, tokens, skysongStats);
        List<SubCommand> commands = List.of(
                new BusinessCommand(businesses, settings.types(), access, vaults, skysongStats, messenger),
                new TokensCommand(tokens, new CharacterArguments(skysongStats, messenger), messenger),
                new ModifierCommand(modifiers, messenger, Clock.systemUTC()));
        BusinessSystem system = new BusinessSystem(businesses, settings.types(), access, vaults, tokens, activities,
                shifts, training, settings.economyLogistics(), ledgers, ledgerSelections, modifiers, pricing, deliveries, menus);
        return new BusinessFeature(system, commands, resources, logger);
    }

    private static Settings readSettings(YamlFiles configFiles, SkysongStatsAdapter skysongStats, Logger logger) {
        BusinessTypeRegistry types = new BusinessTypesParser(skysongStats, logger::warning)
                .parse(configFiles.load(BusinessTypesParser.FILE));
        return new Settings(types,
                new BusinessTiersParser().parse(configFiles.load(BusinessTiersParser.FILE)),
                AccessDefaults.parse(configFiles.load(AccessDefaults.FILE)),
                new ShiftSettingsParser(types, logger::warning).parse(configFiles.load(ShiftSettingsParser.FILE)),
                new TrainingSettingsParser().parse(configFiles.load(TrainingSettingsParser.FILE)),
                new EconomyLogisticsSettingsParser(skysongStats, logger::warning)
                        .parse(configFiles.load(EconomyLogisticsSettingsParser.FILE)));
    }

    /**
     * Reads the {@code ledgers} folder. The example ledger is only created when the folder is
     * missing, so a server that deletes it doesn't get it back.
     */
    private static LedgerRegistry loadLedgers(JavaPlugin plugin, PluginPaths paths, EconomyLogisticsSettings settings,
                                              SkysongItemFactory items) {
        Logger logger = plugin.getLogger();
        if (!paths.ledgers().toFile().exists()) {
            plugin.saveResource(PluginPaths.LEDGERS_FOLDER + "/smugglers.yml", false);
        }
        LedgerParser parser = new LedgerParser(settings.deliveryTimes().keySet(), items::exists, logger::warning);
        LedgerRegistry ledgers = new LedgerLoader(parser, logger::warning).load(paths.ledgers());
        logger.info("Loaded " + ledgers.size() + " trade ledger(s)");
        return ledgers;
    }

    /** CMI is optional: without it, AFK time counts like any other online time. */
    private static AfkCheck afkCheck(JavaPlugin plugin) {
        Plugin cmi = plugin.getServer().getPluginManager().getPlugin("CMI");
        if (cmi == null || !cmi.isEnabled()) {
            return AfkCheck.NONE;
        }
        return CmiAfkCheck.create(cmi.getClass().getClassLoader(), plugin.getLogger());
    }

    /** PlaceholderAPI is optional: without it there are simply no placeholders. */
    private static void registerPlaceholders(JavaPlugin plugin, GatheringTokens tokens, SkysongStatsAdapter skysongStats) {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        TokenPlaceholders placeholders = new TokenPlaceholders(tokens, skysongStats,
                name -> CharacterArguments.findPlayer(name).map(player -> player.getUniqueId()));
        new TokenExpansion(placeholders, plugin.getPluginMeta().getVersion()).register();
        plugin.getLogger().info("Registered the %skysongeconomy_tokens% placeholders");
    }

    private static BusinessMenus createMenus(JavaPlugin plugin, YamlFiles configFiles, BusinessService businesses,
                                             BusinessTypeRegistry types, SkysongStatsAdapter skysongStats,
                                             Messenger messenger, MessageCatalog messages) {
        MenuIcons icons;
        try {
            icons = MenuIcons.parse(configFiles.load(MenuIcons.FILE));
        } catch (ConfigException e) {
            plugin.getLogger().warning("Using the default menu icons: " + e.getMessage());
            icons = MenuIcons.parse(ConfigSection.root(MenuIcons.FILE, null));
        }
        MenuTheme theme = new MenuTheme(icons, messenger, messages, plugin.getLogger());
        ChatPrompts prompts = new ChatPrompts(plugin, messenger, Clock.systemUTC());
        PluginManager events = plugin.getServer().getPluginManager();
        events.registerEvents(new MenuListener(), plugin);
        events.registerEvents(prompts, plugin);
        return new BusinessMenus(businesses, types, skysongStats, theme, prompts, messenger);
    }

    /** A broken block.yml only turns the floating names off. */
    private static NameplateSettings nameplateSettings(YamlFiles configFiles, Logger logger) {
        try {
            return new NameplateSettingsParser().parse(configFiles.load(NameplateSettingsParser.FILE));
        } catch (ConfigException e) {
            logger.warning("Business nameplates are off until this is fixed: " + e.getMessage());
            return new NameplateSettings(false, 0, 1, false, false);
        }
    }

    /** @return the {@code /se business} and {@code /se tokens} commands, none when businesses are disabled */
    public List<SubCommand> commands() {
        return commands;
    }

    /** @return the running business system, empty when businesses are disabled */
    public Optional<BusinessSystem> system() {
        return Optional.ofNullable(system);
    }

    /**
     * Stops the shift and training timer, removes the floating names, waits for pending saves, then closes
     * the database connection.
     */
    @Override
    public void close() {
        for (int i = resources.size() - 1; i >= 0; i--) {
            try {
                resources.get(i).close();
            } catch (Exception e) {
                // Keep closing the rest; a failed close must not leave the database open.
                logger.log(Level.WARNING, "Failed to close " + resources.get(i), e);
            }
        }
    }
}
