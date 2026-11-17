package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.commands.upkeep.UpkeepCommand;
import org.skysongdev.skysongEconomy.commands.upkeep.UpkeepCommandSupport;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.data.database.AsyncUpkeepRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncUpkeepStockRepository;
import org.skysongdev.skysongEconomy.data.database.BackgroundWriter;
import org.skysongdev.skysongEconomy.data.database.CachedGoodTypes;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.SqlUpkeepRepository;
import org.skysongdev.skysongEconomy.data.database.SqlUpkeepStockRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.events.BukkitUpkeepEvents;
import org.skysongdev.skysongEconomy.integration.citizens.CitizensNpcs;
import org.skysongdev.skysongEconomy.integration.skysongnpcs.SkysongNpcsUpkeepAdapter;
import org.skysongdev.skysongEconomy.integration.skysongnpcs.UpkeepNpcListener;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.items.SkysongItemFactory;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.upkeep.StockedItems;
import org.skysongdev.skysongEconomy.upkeep.UpkeepPayment;
import org.skysongdev.skysongEconomy.upkeep.UpkeepService;
import org.skysongdev.skysongEconomy.upkeep.UpkeepSettings;
import org.skysongdev.skysongEconomy.upkeep.UpkeepSettingsParser;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gives NPCs their upkeep: the profiles in {@code npc-upkeep.yml}, storage of each NPC's upkeep
 * and stock, settling cycles every {@code check-every} seconds, and the {@code /se upkeep}
 * commands. Needs SkysongNPCs (and Citizens, which SkysongNPCs is built on), but not businesses:
 * any NPC can have an upkeep. Off without them, or while {@code npc-upkeep.yml} can't be read;
 * saved upkeeps stay saved meanwhile.
 */
public final class UpkeepFeature implements AutoCloseable {
    private static final long TICKS_PER_SECOND = 20;

    private final @Nullable Running running;
    private final List<SubCommand> commands;
    /** Closed in reverse order on shutdown. */
    private final List<AutoCloseable> resources;
    private final Logger logger;

    /**
     * The running upkeep.
     *
     * @param service which NPCs have an upkeep, and their cycles
     * @param stock   the items stocked in them
     * @param npcs    where NPCs stand
     */
    private record Running(UpkeepService service, StockedItems stock, CitizensNpcs npcs) {
    }

    private UpkeepFeature(@Nullable Running running, List<SubCommand> commands, List<AutoCloseable> resources,
                          Logger logger) {
        this.running = running;
        this.commands = List.copyOf(commands);
        this.resources = List.copyOf(resources);
        this.logger = logger;
    }

    /**
     * Reads {@code npc-upkeep.yml} and starts the upkeep.
     *
     * @param database    the database to keep upkeeps in; the feature opens its own connection
     * @param items       the Skysong items there are, to check the profiles' {@code item:} entries
     * @param goodTypes   every item's item categories, for {@code good-type:} entries
     * @param materialIds tells which Skysong item a stocked stack is
     */
    public static UpkeepFeature start(JavaPlugin plugin, YamlFiles configFiles, DatabaseSettings database,
                                      SkysongItemFactory items, CachedGoodTypes goodTypes, MaterialIdResolver materialIds,
                                      Messenger messenger) {
        Logger logger = plugin.getLogger();
        if (!enabled(plugin, NpcFeature.SKYSONG_NPCS) || !enabled(plugin, CitizensNpcs.CITIZENS)) {
            logger.info(NpcFeature.SKYSONG_NPCS + " or " + CitizensNpcs.CITIZENS + " is not installed: NPC upkeep is off");
            return off(logger);
        }
        UpkeepSettings settings;
        try {
            settings = new UpkeepSettingsParser(items::exists, goodTypes::anyItemHas, logger::warning)
                    .parse(configFiles.load(UpkeepSettingsParser.FILE));
        } catch (ConfigException e) {
            logger.severe("NPC upkeep is off until this is fixed: " + e.getMessage());
            return off(logger);
        }
        SkysongNpcsUpkeepAdapter npcs;
        CitizensNpcs citizens;
        try {
            npcs = SkysongNpcsUpkeepAdapter.connect();
            citizens = new CitizensNpcs();
        } catch (RuntimeException | LinkageError e) {
            logger.log(Level.SEVERE, "NPC upkeep is off: " + NpcFeature.SKYSONG_NPCS + " could not be used", e);
            return off(logger);
        }
        List<AutoCloseable> resources = new ArrayList<>();
        ConnectionProvider connections = new ConnectionProvider(database, logger);
        resources.add(connections);
        BackgroundWriter writer = new BackgroundWriter(logger);
        resources.add(writer);
        StockedItems stock = new StockedItems(
                new AsyncUpkeepStockRepository(new SqlUpkeepStockRepository(connections, logger), writer), materialIds);
        Clock clock = Clock.systemUTC();
        UpkeepService service = new UpkeepService(settings,
                new UpkeepService.Dependencies(npcs, stock, new UpkeepPayment(goodTypes)),
                new AsyncUpkeepRepository(new SqlUpkeepRepository(connections, logger), writer),
                new BukkitUpkeepEvents(plugin.getServer().getPluginManager()), clock, logger::warning);
        LastSpots spots = new LastSpots(citizens);
        service.onLost(npc -> spots.dropStock(npc, stock, logger));
        try {
            logger.info("Loaded " + service.loadAll() + " NPC upkeep(s) and " + stock.loadAll(logger::warning)
                    + " upkeep stock(s); " + settings.profiles().size() + " profile(s), "
                    + settings.templates().size() + " template(s) with an upkeep");
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "NPC upkeeps could not be loaded from the database", e);
        }
        Runnable check = () -> {
            try {
                service.syncTemplates();
                spots.remember(service.npcs());
                service.settle();
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "NPC upkeep cycles could not be settled", e);
            }
        };
        check.run();
        long period = settings.checkEvery().toSeconds() * TICKS_PER_SECOND;
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, check, period, period);
        resources.add(task::cancel);
        plugin.getServer().getPluginManager().registerEvents(new UpkeepNpcListener(service), plugin);
        UpkeepCommandSupport support = new UpkeepCommandSupport(service, npcs, citizens::targeted, messenger, clock);
        return new UpkeepFeature(new Running(service, stock, citizens),
                List.of(new UpkeepCommand(support, stock, messenger)), resources, logger);
    }

    private static boolean enabled(Plugin plugin, String name) {
        Plugin other = plugin.getServer().getPluginManager().getPlugin(name);
        return other != null && other.isEnabled();
    }

    private static UpkeepFeature off(Logger logger) {
        return new UpkeepFeature(null, List.of(), List.of(), logger);
    }

    /** @return the {@code /se upkeep} command, or none when upkeep is off */
    public List<SubCommand> commands() {
        return commands;
    }

    /** @return the running upkeep; empty when it is off */
    public Optional<UpkeepService> service() {
        return Optional.ofNullable(running).map(Running::service);
    }

    /** @return the items stocked in NPCs; empty when upkeep is off */
    public Optional<StockedItems> stock() {
        return Optional.ofNullable(running).map(Running::stock);
    }

    /** Stops settling cycles, waits for pending saves, then closes the database connection. */
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

    /**
     * Where each NPC with an upkeep last stood, so the stock of an NPC that loses its upkeep can
     * be dropped there, even after SkysongNPCs deleted it.
     */
    private static final class LastSpots {
        private final CitizensNpcs citizens;
        private final Map<UUID, Location> spots = new HashMap<>();

        LastSpots(CitizensNpcs citizens) {
            this.citizens = citizens;
        }

        void remember(Iterable<UUID> npcs) {
            for (UUID npc : npcs) {
                citizens.location(npc).ifPresent(location -> spots.put(npc, location));
            }
        }

        /**
         * Drops the NPC's stock where it stands or last stood. When it was never seen in a loaded
         * world the stock stays saved, and comes back if the NPC gets an upkeep again.
         */
        void dropStock(UUID npc, StockedItems stock, Logger logger) {
            Location spot = citizens.location(npc).orElse(spots.remove(npc));
            if (stock.items(npc).isEmpty()) {
                return;
            }
            if (spot == null || spot.getWorld() == null) {
                logger.warning("The NPC " + npc + " lost its upkeep but was never seen in a loaded world, "
                        + "so its upkeep stock stays saved until it has an upkeep again");
                return;
            }
            for (ItemStack item : stock.takeAll(npc)) {
                spot.getWorld().dropItemNaturally(spot, item);
            }
        }
    }
}
