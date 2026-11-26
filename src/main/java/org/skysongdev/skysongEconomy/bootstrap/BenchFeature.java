package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongEconomy.api.benches.BusinessBenches;
import org.skysongdev.skysongEconomy.business.bench.BenchAccess;
import org.skysongdev.skysongEconomy.business.bench.BenchLookup;
import org.skysongdev.skysongEconomy.business.bench.BenchRegistry;
import org.skysongdev.skysongEconomy.business.bench.BenchService;
import org.skysongdev.skysongEconomy.business.bench.BenchSettings;
import org.skysongdev.skysongEconomy.business.bench.BenchSettingsParser;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.data.database.AsyncBenchRepository;
import org.skysongdev.skysongEconomy.data.database.BackgroundWriter;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.SqlBenchRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.events.BukkitBenchEvents;
import org.skysongdev.skysongEconomy.integration.skysongcrafting.SkysongCraftingBenches;
import org.skysongdev.skysongEconomy.integration.skysongcrafting.WorkstationAccessListener;
import org.skysongdev.skysongEconomy.integration.skysongcrafting.WorkstationRemovalListener;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SkysongCrafting benches tied to businesses. It only runs while businesses are enabled. The
 * {@link BusinessBenches} service is registered so other plugins can {@code load} it, and every
 * change fires a bench event.
 * <p>
 * SkysongCrafting is optional. Without it, saved links stay and can be read and untied, but nothing
 * new can be tied. No SkysongCrafting class is loaded until it is known to be enabled.
 */
public final class BenchFeature implements AutoCloseable {
    /** The SkysongCrafting plugin's name, as plugin.yml's {@code softdepend} lists it. */
    public static final String SKYSONG_CRAFTING = "SkysongCrafting";

    private final BenchRegistry benches;
    /** Closed in reverse order on shutdown. */
    private final List<AutoCloseable> resources;
    private final Logger logger;

    private BenchFeature(BenchRegistry benches, List<AutoCloseable> resources, Logger logger) {
        this.benches = benches;
        this.resources = List.copyOf(resources);
        this.logger = logger;
    }

    /**
     * Loads the saved links and registers the service; does nothing when businesses are disabled.
     *
     * @param database   the database to keep links in; the feature opens its own connection
     * @param characters whose character a player is playing, for business staff's access to benches
     */
    public static BenchFeature start(JavaPlugin plugin, YamlFiles configFiles, DatabaseSettings database,
                                     Optional<BusinessSystem> businesses, CharacterDirectory characters) {
        Logger logger = plugin.getLogger();
        if (businesses.isEmpty()) {
            return new BenchFeature(null, List.of(), logger);
        }
        BusinessSystem system = businesses.get();
        List<AutoCloseable> resources = new ArrayList<>();
        ConnectionProvider connections = new ConnectionProvider(database, logger);
        resources.add(connections);
        BackgroundWriter writer = new BackgroundWriter(logger);
        resources.add(writer);

        BenchLookup lookup = connect(plugin);
        BenchRegistry benches = new BenchRegistry(system.businesses(),
                new AsyncBenchRepository(new SqlBenchRepository(connections), writer), lookup, Clock.systemUTC());
        try {
            logger.info("Loaded " + benches.loadAll() + " bench(es) tied to businesses");
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Benches tied to businesses could not be loaded from the database", e);
        }
        BenchService service = new BenchService(benches);
        PluginManager events = plugin.getServer().getPluginManager();
        benches.addListener(new BukkitBenchEvents(events, service));
        system.businesses().addListener(benches);
        if (lookup.installed()) {
            events.registerEvents(new WorkstationRemovalListener(benches), plugin);
            events.registerEvents(new WorkstationAccessListener(new BenchAccess(settings(configFiles, logger), benches,
                    system.businesses(), characters)), plugin);
            // One tick later, once every plugin has loaded what it keeps: untie benches that were
            // removed while this plugin wasn't running.
            plugin.getServer().getScheduler().runTask(plugin, () -> benches.removeMissing(link -> logger.info(
                    "Untied the bench at " + link.bench() + " from " + link.businessId() + ": it is no longer a "
                            + SKYSONG_CRAFTING + " bench")));
        }
        plugin.getServer().getServicesManager().register(BusinessBenches.class, service, plugin, ServicePriority.Normal);
        resources.add(() -> plugin.getServer().getServicesManager().unregister(BusinessBenches.class, service));
        return new BenchFeature(benches, resources, logger);
    }

    /** A broken benches.yml only turns business staff's access off. */
    private static BenchSettings settings(YamlFiles configFiles, Logger logger) {
        try {
            return new BenchSettingsParser().parse(configFiles.load(BenchSettingsParser.FILE));
        } catch (ConfigException e) {
            logger.warning("Business staff can't use their benches until this is fixed: " + e.getMessage());
            return BenchSettings.OFF;
        }
    }

    /** @return SkysongCrafting's benches; {@link BenchLookup#NONE}, saying why, when it can't be used */
    private static BenchLookup connect(JavaPlugin plugin) {
        Logger logger = plugin.getLogger();
        Plugin skysongCrafting = plugin.getServer().getPluginManager().getPlugin(SKYSONG_CRAFTING);
        if (skysongCrafting == null || !skysongCrafting.isEnabled()) {
            logger.info(SKYSONG_CRAFTING + " is not installed: benches can't be tied to businesses");
            return BenchLookup.NONE;
        }
        try {
            BenchLookup lookup = SkysongCraftingBenches.connect(plugin);
            logger.info("Connected to " + SKYSONG_CRAFTING + " " + skysongCrafting.getPluginMeta().getVersion());
            return lookup;
        } catch (RuntimeException | LinkageError e) {
            logger.log(Level.SEVERE, "Benches can't be tied to businesses: " + SKYSONG_CRAFTING + " could not be used", e);
            return BenchLookup.NONE;
        }
    }

    /** @return which benches belong to which business; empty when businesses are disabled */
    public Optional<BenchRegistry> benches() {
        return Optional.ofNullable(benches);
    }

    /** Unregisters the service, waits for pending saves, then closes the database connection. */
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
