package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.display.NameplateSettings;
import org.skysongdev.skysongEconomy.business.display.NameplateSettingsParser;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiersParser;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.business.type.BusinessTypesParser;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.commands.business.BusinessCommand;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.data.database.AsyncBusinessRepository;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.SqlBusinessRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.integration.skysongstats.SkysongStatsAdapter;
import org.skysongdev.skysongEconomy.gui.ChatPrompts;
import org.skysongdev.skysongEconomy.gui.MenuListener;
import org.skysongdev.skysongEconomy.gui.MenuTheme;
import org.skysongdev.skysongEconomy.gui.business.BusinessMenus;
import org.skysongdev.skysongEconomy.gui.business.SkillPicksEntry;
import org.skysongdev.skysongEconomy.gui.layout.MenuIcons;
import org.skysongdev.skysongEconomy.listeners.BusinessBlockListener;
import org.skysongdev.skysongEconomy.listeners.BusinessInteractListener;
import org.skysongdev.skysongEconomy.listeners.BusinessNameplates;
import org.skysongdev.skysongEconomy.messaging.MessageCatalog;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.time.Clock;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds the business system and connects it to the server: config, storage, commands and
 * listeners. If its config can't be read the system stays off and the rest of the plugin runs.
 */
public final class BusinessFeature implements AutoCloseable {
    private final @Nullable BusinessService businesses;
    private final @Nullable BusinessTypeRegistry types;
    private final @Nullable SubCommand command;
    private final @Nullable ConnectionProvider connections;
    private final @Nullable AsyncBusinessRepository repository;
    private final @Nullable BusinessNameplates nameplates;
    private final @Nullable BusinessMenus menus;

    private BusinessFeature(@Nullable BusinessService businesses, @Nullable BusinessTypeRegistry types,
                            @Nullable SubCommand command, @Nullable ConnectionProvider connections,
                            @Nullable AsyncBusinessRepository repository, @Nullable BusinessNameplates nameplates,
                            @Nullable BusinessMenus menus) {
        this.businesses = businesses;
        this.types = types;
        this.command = command;
        this.connections = connections;
        this.repository = repository;
        this.nameplates = nameplates;
        this.menus = menus;
    }

    /**
     * Reads the business config, loads every business and registers the block listener.
     *
     * @param database the database to keep businesses in; the feature opens its own connection
     */
    public static BusinessFeature start(JavaPlugin plugin, YamlFiles configFiles, DatabaseSettings database,
                                        SkysongStatsAdapter skysongStats, Messenger messenger, MessageCatalog messages) {
        Logger logger = plugin.getLogger();
        BusinessTypeRegistry types;
        BusinessTiers tiers;
        try {
            types = new BusinessTypesParser(skysongStats, logger::warning).parse(configFiles.load(BusinessTypesParser.FILE));
            tiers = new BusinessTiersParser().parse(configFiles.load(BusinessTiersParser.FILE));
        } catch (ConfigException e) {
            logger.severe("Businesses are disabled until this is fixed: " + e.getMessage());
            return new BusinessFeature(null, null, null, null, null, null, null);
        }

        ConnectionProvider connections = new ConnectionProvider(database, logger);
        AsyncBusinessRepository repository = new AsyncBusinessRepository(new SqlBusinessRepository(connections), logger);
        BusinessService businesses = new BusinessService(types, tiers, repository, Clock.systemUTC());
        PluginManager events = plugin.getServer().getPluginManager();
        BusinessNameplates nameplates = new BusinessNameplates(businesses, nameplateSettings(configFiles, logger), messenger);
        businesses.addListener(nameplates);
        events.registerEvents(nameplates, plugin);
        try {
            logger.info("Loaded " + businesses.loadAll() + " business(es) of " + types.all().size() + " type(s)");
        } catch (StorageException e) {
            // Keep the commands so staff get clear errors, but nothing loaded means nothing to protect yet.
            logger.log(Level.SEVERE, "Businesses could not be loaded from the database", e);
        }

        events.registerEvents(new BusinessBlockListener(businesses, messenger), plugin);
        BusinessMenus menus = createMenus(plugin, configFiles, businesses, types, skysongStats, messenger, messages);
        menus.addEntry(new SkillPicksEntry(menus, businesses, skysongStats, messenger));
        events.registerEvents(new BusinessInteractListener(businesses, menus), plugin);
        SubCommand command = new BusinessCommand(businesses, types, skysongStats, messenger);
        return new BusinessFeature(businesses, types, command, connections, repository, nameplates, menus);
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

    /** @return the {@code /se business} command, empty when businesses are disabled */
    public Optional<SubCommand> command() {
        return Optional.ofNullable(command);
    }

    /** @return every business, empty when businesses are disabled */
    public Optional<BusinessService> businesses() {
        return Optional.ofNullable(businesses);
    }

    /** @return the business menus, to add main menu buttons to; empty when businesses are disabled */
    public Optional<BusinessMenus> menus() {
        return Optional.ofNullable(menus);
    }

    /** @return the business types, empty when businesses are disabled */
    public Optional<BusinessTypeRegistry> types() {
        return Optional.ofNullable(types);
    }

    /** Removes the floating names, waits for pending saves, then closes the database connection. */
    @Override
    public void close() {
        if (nameplates != null) {
            nameplates.close();
        }
        if (repository != null) {
            repository.close();
        }
        if (connections != null) {
            connections.close();
        }
    }
}
