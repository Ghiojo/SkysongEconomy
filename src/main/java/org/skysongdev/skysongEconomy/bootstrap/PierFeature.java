package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongEconomy.api.piers.PierChangeEvent;
import org.skysongdev.skysongEconomy.api.piers.Piers;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.commands.pier.PierCommand;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.data.database.AsyncPierRepository;
import org.skysongdev.skysongEconomy.data.database.BackgroundWriter;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.SqlPierRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.trade.pier.PierService;
import org.skysongdev.skysongEconomy.trade.pier.PierSettings;
import org.skysongdev.skysongEconomy.trade.pier.PierSettingsParser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receiving piers: {@code piers.yml}, piers plugins create, and the {@code /se pier} commands.
 * The {@link Piers} service is registered with Bukkit's services manager so other plugins can
 * {@code load(Piers.class)} it, and every change fires {@link PierChangeEvent}.
 */
public final class PierFeature implements AutoCloseable {
    private final PierService piers;
    private final List<SubCommand> commands;
    /** Closed in reverse order on shutdown. */
    private final List<AutoCloseable> resources;
    private final Logger logger;

    private PierFeature(PierService piers, List<SubCommand> commands, List<AutoCloseable> resources, Logger logger) {
        this.piers = piers;
        this.commands = List.copyOf(commands);
        this.resources = List.copyOf(resources);
        this.logger = logger;
    }

    /**
     * Reads {@code piers.yml}, loads the saved piers and registers the service. A broken file
     * leaves only the piers plugins made, with a console error saying why.
     *
     * @param database the database to keep piers in; the feature opens its own connection
     */
    public static PierFeature start(JavaPlugin plugin, YamlFiles configFiles, DatabaseSettings database,
                                    Messenger messenger) {
        Logger logger = plugin.getLogger();
        List<AutoCloseable> resources = new ArrayList<>();
        ConnectionProvider connections = new ConnectionProvider(database, logger);
        resources.add(connections);
        BackgroundWriter writer = new BackgroundWriter(logger);
        resources.add(writer);
        PierService piers = new PierService(read(configFiles, logger::warning),
                new AsyncPierRepository(new SqlPierRepository(connections), writer), logger::warning);
        try {
            piers.loadAll();
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Piers saved in the database could not be loaded", e);
        }
        logger.info("Loaded " + piers.all().size() + " pier(s)");
        piers.addListener((before, after) -> plugin.getServer().getPluginManager().callEvent(new PierChangeEvent(before, after)));
        plugin.getServer().getServicesManager().register(Piers.class, piers, plugin, ServicePriority.Normal);
        resources.add(() -> plugin.getServer().getServicesManager().unregister(Piers.class, piers));
        PierCommand command = new PierCommand(piers, messenger, () -> {
            int[] warnings = {0};
            piers.reload(read(configFiles, warning -> {
                warnings[0]++;
                logger.warning(warning);
            }));
            return warnings[0];
        });
        return new PierFeature(piers, List.of(command), resources, logger);
    }

    /** @return what {@code piers.yml} holds; no piers when it can't be read */
    private static PierSettings read(YamlFiles configFiles, Consumer<String> warnings) {
        try {
            return new PierSettingsParser(warnings).parse(configFiles.load(PierSettingsParser.FILE));
        } catch (ConfigException e) {
            warnings.accept("Piers from " + PierSettingsParser.FILE + " are off until it is fixed: " + e.getMessage());
            return PierSettings.NONE;
        }
    }

    /** @return every receiving pier, and the way to manage them */
    public PierService piers() {
        return piers;
    }

    /** @return the {@code /se pier} command */
    public List<SubCommand> commands() {
        return commands;
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
