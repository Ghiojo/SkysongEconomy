package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.Module;
import org.skysongdev.skysongEconomy.business.module.ModuleParser;
import org.skysongdev.skysongEconomy.business.module.ModuleSettings;
import org.skysongdev.skysongEconomy.business.module.ModuleSettingsParser;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.FileRegistry;
import org.skysongdev.skysongEconomy.config.PluginPaths;
import org.skysongdev.skysongEconomy.config.ResourceExtractor;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.config.YamlFolder;
import org.skysongdev.skysongEconomy.data.database.AsyncModulePickRepository;
import org.skysongdev.skysongEconomy.data.database.BackgroundWriter;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.SqlModulePickRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.events.BukkitBusinessModuleEvents;
import org.skysongdev.skysongEconomy.gui.business.ModulesEntry;
import org.skysongdev.skysongEconomy.integration.skysongstats.SkysongStatsAdapter;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.io.File;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gives businesses their modules: the module files in {@code modules/}, the picks per skill in
 * {@code businesses/modules.yml}, storage, the Modules button and the {@code /se business
 * modules} and {@code clearmodule} commands. Off when businesses are disabled or
 * {@code businesses/modules.yml} can't be read; picked modules stay saved meanwhile.
 */
public final class ModuleFeature implements AutoCloseable {
    private final @Nullable BusinessModules modules;
    /** Closed in reverse order on shutdown. */
    private final List<AutoCloseable> resources;
    private final Logger logger;

    private ModuleFeature(@Nullable BusinessModules modules, List<AutoCloseable> resources, Logger logger) {
        this.modules = modules;
        this.resources = List.copyOf(resources);
        this.logger = logger;
    }

    /**
     * Reads the module files and starts the businesses' module picks.
     *
     * @param jarFile      the plugin jar, from {@code JavaPlugin#getFile()}, for the example module file
     * @param database     the database to keep module picks in; the feature opens its own connection
     * @param businesses   the running business system, empty when businesses are disabled
     * @param skysongStats the skills {@code modules.yml} names
     */
    public static ModuleFeature start(JavaPlugin plugin, File jarFile, PluginPaths paths, YamlFiles configFiles,
                                      DatabaseSettings database, Optional<BusinessSystem> businesses,
                                      SkysongStatsAdapter skysongStats, Messenger messenger) {
        Logger logger = plugin.getLogger();
        if (businesses.isEmpty()) {
            return new ModuleFeature(null, List.of(), logger);
        }
        BusinessSystem system = businesses.get();
        new ResourceExtractor(plugin, jarFile).extractFolder(PluginPaths.MODULES_FOLDER);
        ModuleSettings settings;
        try {
            FileRegistry<Module> files = new YamlFolder(ModuleParser.FOLDER, "module", logger::warning)
                    .parseEach(paths.modules(), new ModuleParser(logger::warning)::parse);
            settings = new ModuleSettingsParser(skysongStats, logger::warning)
                    .parse(configFiles.load(ModuleSettingsParser.FILE), files);
            logger.info("Loaded " + files.size() + " module(s); " + settings.skills().skills().size()
                    + " skill(s) give module picks");
        } catch (ConfigException e) {
            logger.severe("Business modules are off until this is fixed: " + e.getMessage());
            return new ModuleFeature(null, List.of(), logger);
        }
        List<AutoCloseable> resources = new ArrayList<>();
        ConnectionProvider connections = new ConnectionProvider(database, logger);
        resources.add(connections);
        BackgroundWriter writer = new BackgroundWriter(logger);
        resources.add(writer);
        BusinessModules modules = new BusinessModules(settings, system.businesses(),
                new AsyncModulePickRepository(new SqlModulePickRepository(connections), writer),
                new BukkitBusinessModuleEvents(plugin.getServer().getPluginManager()), Clock.systemUTC(), logger::warning);
        try {
            logger.info("Loaded " + modules.loadAll() + " module pick(s)");
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Module picks could not be loaded from the database", e);
        }
        // After loading, so businesses that change from now on keep their modules in line.
        system.businesses().addListener(modules);
        system.menus().addEntry(new ModulesEntry(system.menus(), modules, skysongStats, messenger));
        system.command().enableModules(modules, skysongStats);
        return new ModuleFeature(modules, resources, logger);
    }

    /** @return the modules each business picked; empty when businesses are disabled or {@code businesses/modules.yml} can't be read */
    public Optional<BusinessModules> modules() {
        return Optional.ofNullable(modules);
    }

    /** Waits for pending saves, then closes the database connection. */
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
