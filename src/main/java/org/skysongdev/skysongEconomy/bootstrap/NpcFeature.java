package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.npc.BusinessGroupDirectory;
import org.skysongdev.skysongEconomy.business.npc.BusinessGroupRef;
import org.skysongdev.skysongEconomy.business.npc.NpcAccessGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.NpcSettings;
import org.skysongdev.skysongEconomy.business.npc.NpcSettingsParser;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.data.database.AsyncBusinessNpcRepository;
import org.skysongdev.skysongEconomy.data.database.BackgroundWriter;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.SqlBusinessNpcRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.events.BukkitBusinessNpcEvents;
import org.skysongdev.skysongEconomy.gui.business.NpcsEntry;
import org.skysongdev.skysongEconomy.integration.skysongnpcs.BusinessGroupProvider;
import org.skysongdev.skysongEconomy.integration.skysongnpcs.NpcRemovalListener;
import org.skysongdev.skysongEconomy.integration.skysongnpcs.SkysongNpcsAdapter;
import org.skysongdev.skysongEconomy.integration.skysongstats.SkysongStatsAdapter;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongstats.api.SkysongStatsApi;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connects businesses to SkysongNPCs, which is optional. With SkysongNPCs installed it opens the
 * gateways the business NPC features use, lets SkysongNPCs name business access groups as
 * {@code economy:<business>/<group>}, and gives businesses their NPC slots: the settings in
 * {@code businesses/npcs.yml}, storage, the NPCs button and the {@code /se business npcs} and
 * {@code dismissnpc} commands. Without SkysongNPCs, or when businesses are off, there are no
 * gateways and everything that needs NPCs stays off.
 */
public final class NpcFeature implements AutoCloseable {
    /** The SkysongNPCs plugin's name, as plugin.yml's {@code softdepend} lists it. */
    public static final String SKYSONG_NPCS = "SkysongNPCs";

    private final @Nullable Gateways gateways;
    private final @Nullable BusinessNpcs businessNpcs;
    /** Closed in reverse order on shutdown. */
    private final List<AutoCloseable> resources;
    private final Logger logger;

    private NpcFeature(@Nullable Gateways gateways, @Nullable BusinessNpcs businessNpcs, List<AutoCloseable> resources,
                       Logger logger) {
        this.gateways = gateways;
        this.businessNpcs = businessNpcs;
        this.resources = List.copyOf(resources);
        this.logger = logger;
    }

    /**
     * The ways into SkysongNPCs.
     *
     * @param npcs   creating, removing and owning NPCs
     * @param sheets what an NPC was given
     * @param access who may do what with a business's NPCs
     */
    public record Gateways(NpcGateway npcs, NpcSheetGateway sheets, NpcAccessGateway access) {
    }

    /**
     * Connects to SkysongNPCs when it is enabled, registers the business groups with it and
     * starts the businesses' NPC slots.
     *
     * @param database     the database to keep business NPCs in; the feature opens its own connection
     * @param businesses   the running business system, empty when businesses are disabled
     * @param skysongStats the character a player is playing, and the skills {@code npcs.yml} names
     */
    public static NpcFeature start(JavaPlugin plugin, YamlFiles configFiles, DatabaseSettings database,
                                   Optional<BusinessSystem> businesses, SkysongStatsAdapter skysongStats,
                                   Messenger messenger) {
        Logger logger = plugin.getLogger();
        if (businesses.isEmpty()) {
            return off(logger);
        }
        Plugin skysongNpcs = plugin.getServer().getPluginManager().getPlugin(SKYSONG_NPCS);
        if (skysongNpcs == null || !skysongNpcs.isEnabled()) {
            logger.info(SKYSONG_NPCS + " is not installed: business NPCs are off");
            return off(logger);
        }
        BusinessSystem system = businesses.get();
        SkysongNpcsAdapter adapter;
        try {
            adapter = SkysongNpcsAdapter.connect(plugin, SkysongStatsApi.get());
            adapter.registerGroups(BusinessGroupRef.TYPE, new BusinessGroupProvider(
                    new BusinessGroupDirectory(system.businesses(), system.access()), skysongStats));
            logger.info("Connected to " + SKYSONG_NPCS + ": business groups are available to NPC access as "
                    + BusinessGroupRef.TYPE + ":<business>/<group>");
        } catch (RuntimeException | LinkageError e) {
            logger.log(Level.SEVERE, "Business NPCs are off: " + SKYSONG_NPCS + " could not be used", e);
            return off(logger);
        }
        Gateways gateways = new Gateways(adapter, adapter, adapter);
        List<AutoCloseable> resources = new ArrayList<>();
        BusinessNpcs businessNpcs = startSlots(plugin, configFiles, database, system, gateways, skysongStats, messenger,
                resources);
        return new NpcFeature(gateways, businessNpcs, resources, logger);
    }

    /**
     * Gives businesses their NPC slots. A {@code businesses/npcs.yml} that can't be read leaves
     * every NPC as it is and turns the slots off until it is fixed.
     *
     * @param resources collects what must be closed on shutdown
     * @return the running slots, {@code null} when the file can't be read
     */
    private static @Nullable BusinessNpcs startSlots(JavaPlugin plugin, YamlFiles configFiles, DatabaseSettings database,
                                                     BusinessSystem system, Gateways gateways,
                                                     SkysongStatsAdapter skysongStats, Messenger messenger,
                                                     List<AutoCloseable> resources) {
        Logger logger = plugin.getLogger();
        NpcSettings settings;
        try {
            settings = new NpcSettingsParser(skysongStats, logger::warning).parse(configFiles.load(NpcSettingsParser.FILE));
        } catch (ConfigException e) {
            logger.severe("Business NPC slots are off until this is fixed: " + e.getMessage());
            return null;
        }
        ConnectionProvider connections = new ConnectionProvider(database, logger);
        resources.add(connections);
        BackgroundWriter writer = new BackgroundWriter(logger);
        resources.add(writer);

        PluginManager events = plugin.getServer().getPluginManager();
        BusinessNpcs businessNpcs = new BusinessNpcs(settings,
                new BusinessNpcs.Dependencies(system.businesses(), system.access(), gateways.npcs()),
                new AsyncBusinessNpcRepository(new SqlBusinessNpcRepository(connections, logger), writer),
                new BukkitBusinessNpcEvents(events), Clock.systemUTC(), logger::warning);
        try {
            logger.info("Loaded " + businessNpcs.loadAll() + " business NPC(s)");
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Business NPCs could not be loaded from the database", e);
        }
        // After loading, so businesses that change from now on keep their NPCs in line.
        system.businesses().addListener(businessNpcs);
        events.registerEvents(new NpcRemovalListener(businessNpcs), plugin);
        system.menus().addEntry(new NpcsEntry(system.menus(), businessNpcs, gateways.npcs(), gateways.sheets(), settings,
                skysongStats, messenger));
        system.command().enableNpcs(businessNpcs, gateways.npcs(), gateways.sheets(), skysongStats);
        return businessNpcs;
    }

    private static NpcFeature off(Logger logger) {
        return new NpcFeature(null, null, List.of(), logger);
    }

    /** @return the gateways into SkysongNPCs, empty when it is not installed or businesses are disabled */
    public Optional<Gateways> gateways() {
        return Optional.ofNullable(gateways);
    }

    /**
     * @return the NPCs each business has, empty when SkysongNPCs is not installed, businesses
     *         are disabled or {@code businesses/npcs.yml} can't be read
     */
    public Optional<BusinessNpcs> businessNpcs() {
        return Optional.ofNullable(businessNpcs);
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
