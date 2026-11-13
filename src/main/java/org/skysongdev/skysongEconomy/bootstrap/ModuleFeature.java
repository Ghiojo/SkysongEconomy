package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.Module;
import org.skysongdev.skysongEconomy.business.module.ModuleParser;
import org.skysongdev.skysongEconomy.business.module.ModuleSettings;
import org.skysongdev.skysongEconomy.business.module.ModuleSettingsParser;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleEffectParsers;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleWorkerSlots;
import org.skysongdev.skysongEconomy.business.module.npc.ModuleTargets;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.business.npc.benefit.BenefitParser;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcProgression;
import org.skysongdev.skysongEconomy.commands.business.BusinessCommand;
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
 * modules} and {@code clearmodule} commands, and the modules' effects: extra worker slots here,
 * NPC slots and benefits once {@link NpcFeature} runs. Off when businesses are disabled or
 * {@code businesses/modules.yml} can't be read; picked modules stay saved meanwhile.
 */
public final class ModuleFeature implements AutoCloseable {
    private final @Nullable BusinessModules modules;
    private final @Nullable NpcHooks hooks;
    /** Closed in reverse order on shutdown. */
    private final List<AutoCloseable> resources;
    private final Logger logger;

    /** Where the modules' NPC side is switched on once the business NPCs run. */
    private record NpcHooks(ModulesEntry entry, BusinessCommand command) {
    }

    private ModuleFeature(@Nullable BusinessModules modules, @Nullable NpcHooks hooks, List<AutoCloseable> resources,
                          Logger logger) {
        this.modules = modules;
        this.hooks = hooks;
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
     * @param npcSheets    SkysongNPCs, to check the NPC effects of module files; empty when it is
     *                     not installed, and those effects then do nothing
     */
    public static ModuleFeature start(JavaPlugin plugin, File jarFile, PluginPaths paths, YamlFiles configFiles,
                                      DatabaseSettings database, Optional<BusinessSystem> businesses,
                                      SkysongStatsAdapter skysongStats, Messenger messenger,
                                      Optional<NpcSheetGateway> npcSheets) {
        Logger logger = plugin.getLogger();
        if (businesses.isEmpty()) {
            return new ModuleFeature(null, null, List.of(), logger);
        }
        BusinessSystem system = businesses.get();
        ModuleEffectParsers effects = ModuleEffectParsers.basic();
        if (npcSheets.isPresent()) {
            effects.withNpcEffects(new BenefitParser(skysongStats, npcSheets.get()), skysongStats);
        } else {
            effects.withoutNpcEffects();
        }
        new ResourceExtractor(plugin, jarFile).extractFolder(PluginPaths.MODULES_FOLDER);
        ModuleSettings settings;
        try {
            FileRegistry<Module> files = new YamlFolder(ModuleParser.FOLDER, "module", logger::warning)
                    .parseEach(paths.modules(), new ModuleParser(effects, logger::warning)::parse);
            settings = new ModuleSettingsParser(skysongStats, logger::warning)
                    .parse(configFiles.load(ModuleSettingsParser.FILE), files);
            logger.info("Loaded " + files.size() + " module(s); " + settings.skills().skills().size()
                    + " skill(s) give module picks");
        } catch (ConfigException e) {
            logger.severe("Business modules are off until this is fixed: " + e.getMessage());
            return new ModuleFeature(null, null, List.of(), logger);
        }
        List<AutoCloseable> resources = new ArrayList<>();
        ConnectionProvider connections = new ConnectionProvider(database, logger);
        resources.add(connections);
        BackgroundWriter writer = new BackgroundWriter(logger);
        resources.add(writer);
        BusinessModules modules = new BusinessModules(settings, system.businesses(),
                new AsyncModulePickRepository(new SqlModulePickRepository(connections), writer),
                new BukkitBusinessModuleEvents(plugin.getServer().getPluginManager()), Clock.systemUTC(), logger::warning);
        // Before loading, so a pick that is dropped on load takes its extra workers with it.
        ModuleWorkerSlots workers = new ModuleWorkerSlots(modules, system.businesses());
        system.businesses().addWorkerSlots(workers::extra);
        modules.addListener(workers);
        try {
            logger.info("Loaded " + modules.loadAll() + " module pick(s)");
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Module picks could not be loaded from the database", e);
        }
        // After loading, so businesses that change from now on keep their modules in line.
        system.businesses().addListener(modules);
        ModulesEntry entry = new ModulesEntry(system.menus(), modules, skysongStats, messenger);
        system.menus().addEntry(entry);
        system.command().enableModules(modules, workers, skysongStats);
        return new ModuleFeature(modules, new NpcHooks(entry, system.command()), resources, logger);
    }

    /** @return the modules each business picked; empty when businesses are disabled or {@code businesses/modules.yml} can't be read */
    public Optional<BusinessModules> modules() {
        return Optional.ofNullable(modules);
    }

    /**
     * Switches on what modules do with NPCs in the menus and commands: choosing the NPCs for a
     * benefit, and saying how many are still to choose. Called once the business NPCs run.
     *
     * @param targets     which NPCs were chosen for which benefit
     * @param npcs        SkysongNPCs, for names
     * @param progression gives a chosen NPC its benefit at once
     */
    public void enableNpcs(ModuleTargets targets, NpcGateway npcs, NpcProgression progression) {
        if (hooks != null) {
            hooks.entry().enableNpcs(targets, npcs, progression);
            hooks.command().enableModuleNpcs(targets);
        }
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
