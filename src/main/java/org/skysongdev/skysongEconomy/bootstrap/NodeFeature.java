package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.commands.node.NodeCommand;
import org.skysongdev.skysongEconomy.commands.node.OddsSources;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.data.database.AsyncNodeRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncPlantingRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncPrimerReturnRepository;
import org.skysongdev.skysongEconomy.data.database.BackgroundWriter;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.SqlNodeRepository;
import org.skysongdev.skysongEconomy.data.database.SqlPlantingRepository;
import org.skysongdev.skysongEconomy.data.database.SqlPrimerReturnRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.events.BukkitNodeHarvestEvents;
import org.skysongdev.skysongEconomy.gui.node.NodeMenus;
import org.skysongdev.skysongEconomy.integration.skysongstats.SkysongStatsAdapter;
import org.skysongdev.skysongEconomy.items.InventoryPrimerDelivery;
import org.skysongdev.skysongEconomy.items.ItemHandOver;
import org.skysongdev.skysongEconomy.items.PrimerItems;
import org.skysongdev.skysongEconomy.listeners.FloatingNames;
import org.skysongdev.skysongEconomy.listeners.NodeInteractListener;
import org.skysongdev.skysongEconomy.listeners.NodeItemTags;
import org.skysongdev.skysongEconomy.listeners.NodeNameplates;
import org.skysongdev.skysongEconomy.listeners.PrimerReturnListener;
import org.skysongdev.skysongEconomy.listeners.ProtectedBlockListener;
import org.skysongdev.skysongEconomy.loot.LootRoller;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.nodes.NodeBlockGuard;
import org.skysongdev.skysongEconomy.nodes.NodeFiles;
import org.skysongdev.skysongEconomy.nodes.NodeService;
import org.skysongdev.skysongEconomy.nodes.NodeSettings;
import org.skysongdev.skysongEconomy.nodes.NodeSettingsParser;
import org.skysongdev.skysongEconomy.nodes.NodeTables;
import org.skysongdev.skysongEconomy.nodes.farm.FarmHarvester;
import org.skysongdev.skysongEconomy.nodes.farm.FarmPlots;
import org.skysongdev.skysongEconomy.nodes.farm.PrimerReturns;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestPayer;
import org.skysongdev.skysongEconomy.nodes.harvest.NodeAccess;
import org.skysongdev.skysongEconomy.nodes.harvest.RandomHarvester;
import org.skysongdev.skysongitems.api.SkysongItemsApi;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds the nodes system and connects it to the server: settings, the node files (loot groups,
 * random tables, farm resources and farm types), storage, block protection, nameplates, the
 * random and farm node menus, giving primers back, the node item tags and the {@code /se node}
 * commands. Nodes need businesses, so the
 * system stays off when businesses are off or {@code nodes/settings.yml} can't be read.
 */
public final class NodeFeature implements AutoCloseable {
    private final List<SubCommand> commands;
    /** Closed in reverse order on shutdown. */
    private final List<AutoCloseable> resources;
    private final Optional<NodeService> nodes;
    private final Logger logger;

    private NodeFeature(Optional<NodeService> nodes, List<SubCommand> commands, List<AutoCloseable> resources,
                        Logger logger) {
        this.nodes = nodes;
        this.commands = List.copyOf(commands);
        this.resources = List.copyOf(resources);
        this.logger = logger;
    }

    /**
     * Reads {@code nodes/settings.yml}, loads every node and registers the listeners and commands.
     *
     * @param database     the database to keep nodes in; the feature opens its own connection
     * @param businesses   the running business system, empty when businesses are disabled
     * @param skysongStats skills and skill levels, which tables name
     * @param wallet       money, for public costs and how amounts are written
     * @param trade        items, which loot names, and the price database, for {@code /se node odds}
     */
    public static NodeFeature start(JavaPlugin plugin, YamlFiles configFiles, DatabaseSettings database,
                                    Optional<BusinessSystem> businesses, SkysongStatsAdapter skysongStats,
                                    Wallet wallet, TradeInputs trade, Messenger messenger) {
        Logger logger = plugin.getLogger();
        if (businesses.isEmpty()) {
            logger.severe("Nodes are disabled because businesses are disabled");
            return disabled(logger);
        }
        BusinessSystem system = businesses.get();
        NodeSettings settings;
        try {
            settings = new NodeSettingsParser(type -> system.types().find(type).isPresent(), skysongStats::levelExists,
                    logger::warning)
                    .parse(configFiles.load(NodeSettingsParser.FILE));
        } catch (ConfigException e) {
            logger.severe("Nodes are disabled until this is fixed: " + e.getMessage());
            return disabled(logger);
        }

        List<AutoCloseable> resources = new ArrayList<>();
        ConnectionProvider connections = new ConnectionProvider(database, logger);
        resources.add(connections);
        BackgroundWriter writer = new BackgroundWriter(logger);
        resources.add(writer);

        NodeFiles files = new NodeFiles(plugin.getDataFolder().toPath(), trade.items()::exists, skysongStats::exists,
                logger::warning);
        NodeFiles.Loaded loaded = files.load();
        logLoaded(logger, loaded);
        NodeTables tables = NodeTables.of(loaded);
        NodeService nodes = new NodeService(tables, settings, system.businesses(),
                new AsyncNodeRepository(new SqlNodeRepository(connections), writer));
        system.businesses().addListener(nodes);
        // A business can't be created or moved onto a node's block, and the other way round.
        system.businesses().addBlockGuard(new NodeBlockGuard(nodes));

        PluginManager events = plugin.getServer().getPluginManager();
        ItemHandOver handOver = new ItemHandOver(trade.items());
        NodeAccess access = new NodeAccess(system.businesses());
        PrimerReturns returns = new PrimerReturns(
                new AsyncPrimerReturnRepository(new SqlPrimerReturnRepository(connections, logger), writer),
                new InventoryPrimerDelivery(plugin.getServer(), handOver, messenger));
        FarmPlots plots = new FarmPlots(new FarmPlots.Dependencies(tables, settings, access, system.access(), skysongStats,
                new AsyncPlantingRepository(new SqlPlantingRepository(connections, logger), writer), returns));
        FloatingNames nameplates = new FloatingNames(settings.nameplate());
        NodeNameplates nodeNameplates = new NodeNameplates(nodes, nameplates, messenger, plots::plantedName);
        nodes.addListener(nodeNameplates);
        // After the nameplates, so a primer given back on a change shows on the nameplate at once.
        nodes.addListener(plots);
        plots.addListener(nodeNameplates::refresh);
        events.registerEvents(nameplates, plugin);
        resources.add(nameplates);
        try {
            logger.info("Loaded " + returns.loadAll() + " primer(s) waiting for their planters and "
                    + plots.loadAll() + " planted farm node(s)");
            // Plantings load first, so ones whose node is gone or changed are given back now.
            logger.info("Loaded " + nodes.loadAll() + " node(s)");
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Nodes could not be loaded from the database", e);
        }
        events.registerEvents(new NodeItemTags(plugin, logger), plugin);
        events.registerEvents(new PrimerReturnListener(returns), plugin);
        events.registerEvents(new ProtectedBlockListener(List.of(new NodeBlockGuard(nodes)), messenger), plugin);

        HarvestPayer payer = new HarvestPayer(system.tokens(), skysongStats, wallet);
        BukkitNodeHarvestEvents harvestEvents = new BukkitNodeHarvestEvents(events);
        LootRoller roller = new LootRoller(new SecureRandom());
        RandomHarvester harvester = new RandomHarvester(new RandomHarvester.Dependencies(tables, settings, access, payer,
                skysongStats, harvestEvents), roller);
        FarmHarvester farmHarvester = new FarmHarvester(new FarmHarvester.Dependencies(plots, settings, access, payer,
                skysongStats, harvestEvents), roller);
        NodeMenus.Farms farms = new NodeMenus.Farms(plots, farmHarvester,
                new PrimerItems(SkysongItemsApi.get().tags(), trade.materialIds()));
        NodeMenus menus = new NodeMenus(new NodeMenus.Dependencies(nodes, harvester, farms, system.businesses(),
                skysongStats, system.tokens(), skysongStats, skysongStats, wallet, handOver),
                system.menus().theme(), messenger, settings.menuIcons(), logger);
        events.registerEvents(new NodeInteractListener(nodes, menus), plugin);

        OddsSources odds = new OddsSources(tables, settings, skysongStats, skysongStats, trade.prices(),
                trade.priceSettings(), wallet);
        Supplier<NodeFiles.Loaded> reload = () -> {
            NodeFiles.Loaded reloaded = files.load();
            logLoaded(logger, reloaded);
            tables.replace(reloaded);
            nodes.refresh();
            return reloaded;
        };
        List<SubCommand> commands = List.of(new NodeCommand(nodes, system.businesses(), messenger, odds, reload));
        return new NodeFeature(Optional.of(nodes), commands, resources, logger);
    }

    private static void logLoaded(Logger logger, NodeFiles.Loaded loaded) {
        logger.info("Loaded " + loaded.groups().ids().size() + " loot group(s), " + loaded.random().size()
                + " random node table(s), " + loaded.farmResources().size() + " farm resource(s) and "
                + loaded.farmTypes().size() + " farm type(s)");
    }

    private static NodeFeature disabled(Logger logger) {
        return new NodeFeature(Optional.empty(), List.of(), List.of(), logger);
    }

    /** @return the {@code /se node} command, none when nodes are disabled */
    public List<SubCommand> commands() {
        return commands;
    }

    /** @return the running node service, empty when nodes are disabled */
    public Optional<NodeService> nodes() {
        return nodes;
    }

    /** Removes the floating names, waits for pending saves, then closes the database connection. */
    @Override
    public void close() {
        for (int i = resources.size() - 1; i >= 0; i--) {
            try {
                resources.get(i).close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to close " + resources.get(i), e);
            }
        }
    }
}
