package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.commands.node.NodeCommand;
import org.skysongdev.skysongEconomy.commands.node.OddsSources;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.data.database.AsyncNodeRepository;
import org.skysongdev.skysongEconomy.data.database.BackgroundWriter;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.SqlNodeRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.integration.skysongstats.SkysongStatsAdapter;
import org.skysongdev.skysongEconomy.listeners.FloatingNames;
import org.skysongdev.skysongEconomy.listeners.NodeNameplates;
import org.skysongdev.skysongEconomy.listeners.ProtectedBlockListener;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.nodes.NodeBlockGuard;
import org.skysongdev.skysongEconomy.nodes.NodeFiles;
import org.skysongdev.skysongEconomy.nodes.NodeService;
import org.skysongdev.skysongEconomy.nodes.NodeSettings;
import org.skysongdev.skysongEconomy.nodes.NodeSettingsParser;
import org.skysongdev.skysongEconomy.nodes.NodeTables;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds the nodes system and connects it to the server: settings, loot groups and random tables,
 * storage, block protection, nameplates and the {@code /se node} commands. Nodes need businesses, so the system stays off
 * when businesses are off or {@code nodes/settings.yml} can't be read.
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
     * @param wallet       how money amounts are written
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
        NodeTables tables = new NodeTables(loaded.random());
        NodeService nodes = new NodeService(tables, settings, system.businesses(),
                new AsyncNodeRepository(new SqlNodeRepository(connections), writer));
        system.businesses().addListener(nodes);
        // A business can't be created or moved onto a node's block, and the other way round.
        system.businesses().addBlockGuard(new NodeBlockGuard(nodes));

        PluginManager events = plugin.getServer().getPluginManager();
        FloatingNames nameplates = new FloatingNames(settings.nameplate());
        nodes.addListener(new NodeNameplates(nodes, nameplates, messenger));
        events.registerEvents(nameplates, plugin);
        resources.add(nameplates);
        try {
            logger.info("Loaded " + nodes.loadAll() + " node(s)");
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Nodes could not be loaded from the database", e);
        }
        events.registerEvents(new ProtectedBlockListener(List.of(new NodeBlockGuard(nodes)), messenger), plugin);

        OddsSources odds = new OddsSources(tables, settings, skysongStats, skysongStats, trade.prices(),
                trade.priceSettings(), wallet);
        Supplier<NodeFiles.Loaded> reload = () -> {
            NodeFiles.Loaded reloaded = files.load();
            logLoaded(logger, reloaded);
            tables.replace(reloaded.random());
            nodes.refresh();
            return reloaded;
        };
        List<SubCommand> commands = List.of(new NodeCommand(nodes, system.businesses(), messenger, odds, reload));
        return new NodeFeature(Optional.of(nodes), commands, resources, logger);
    }

    private static void logLoaded(Logger logger, NodeFiles.Loaded loaded) {
        logger.info("Loaded " + loaded.groups().ids().size() + " loot group(s) and " + loaded.random().size()
                + " random node table(s)");
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
