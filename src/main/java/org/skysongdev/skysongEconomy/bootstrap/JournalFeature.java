package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongEconomy.api.business.Businesses;
import org.skysongdev.skysongEconomy.api.journal.EconomyJournal;
import org.skysongdev.skysongEconomy.api.journal.EconomyRecordedEvent;
import org.skysongdev.skysongEconomy.business.BusinessDirectory;
import org.skysongdev.skysongEconomy.calendar.CalendarClock;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.data.database.AsyncJournalRepository;
import org.skysongdev.skysongEconomy.data.database.BackgroundWriter;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.SqlJournalRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.events.JournalListener;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.journal.EconomyRecorder;
import org.skysongdev.skysongEconomy.journal.JournalService;
import org.skysongdev.skysongEconomy.journal.JournalSettings;
import org.skysongdev.skysongEconomy.journal.LedgerTradeNames;
import org.skysongdev.skysongEconomy.journal.TradeNames;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The economy journal: {@code journal.yml}, the {@code economy_journal} table, the listener that
 * records SkysongEconomy's own events, and the {@link EconomyJournal} service other plugins
 * {@code load(EconomyJournal.class)} to read it. Start it before the features whose start-up can record
 * something (farm primers given back when nodes load).
 */
public final class JournalFeature implements AutoCloseable {
    private final Optional<EconomyRecorder> recorder;
    private final Optional<JournalService> journal;
    /** Closed in reverse order on shutdown. */
    private final List<AutoCloseable> resources;
    private final Logger logger;

    private JournalFeature(Optional<EconomyRecorder> recorder, Optional<JournalService> journal,
                           List<AutoCloseable> resources, Logger logger) {
        this.recorder = recorder;
        this.journal = journal;
        this.resources = List.copyOf(resources);
        this.logger = logger;
    }

    /**
     * Reads {@code journal.yml} and, unless the journal is off, starts recording. A broken file keeps
     * the default: record everything, forever.
     *
     * @param businesses the business system, for business and ledger names; empty when businesses are off
     */
    public static JournalFeature start(JavaPlugin plugin, YamlFiles configFiles, DatabaseSettings database,
                                       CalendarClock calendar, Optional<BusinessSystem> businesses,
                                       MaterialIdResolver materialIds) {
        Logger logger = plugin.getLogger();
        JournalSettings settings = read(configFiles, logger);
        if (!settings.enabled()) {
            logger.info("The economy journal is off (" + JournalSettings.FILE + "): nothing is recorded");
            return new JournalFeature(Optional.empty(), Optional.empty(), List.of(), logger);
        }
        List<AutoCloseable> resources = new ArrayList<>();
        ConnectionProvider connections = new ConnectionProvider(database, logger);
        resources.add(connections);
        BackgroundWriter writer = new BackgroundWriter(logger);
        resources.add(writer);
        AsyncJournalRepository repository = new AsyncJournalRepository(new SqlJournalRepository(connections), writer);
        Optional<Businesses> directory = businesses.map(system -> new BusinessDirectory(system.businesses()));
        TradeNames names = businesses.<TradeNames>map(system -> new LedgerTradeNames(system.ledgers()))
                .orElse(TradeNames.NONE);
        EconomyRecorder recorder = new EconomyRecorder(new EconomyRecorder.Dependencies(repository,
                record -> plugin.getServer().getPluginManager().callEvent(new EconomyRecordedEvent(record)),
                calendar, directory, names, Clock.systemUTC()));
        try {
            recorder.loadSequence();
            logger.info("The economy journal carries on after record " + recorder.latestSequence());
        } catch (StorageException e) {
            // Numbering again from 1 would clash with saved records, so record nothing this run.
            logger.log(Level.SEVERE, "The economy journal could not be read, so nothing is recorded until the "
                    + "database is reachable and the plugin restarts", e);
            return new JournalFeature(Optional.empty(), Optional.empty(), resources, logger);
        }
        if (settings.keepDays() > 0) {
            repository.deleteBefore(Clock.systemUTC().instant().minus(Duration.ofDays(settings.keepDays())));
        }
        plugin.getServer().getPluginManager().registerEvents(
                new JournalListener(recorder, materialIds::fromItem, logger), plugin);
        JournalService journal = new JournalService(repository::queryAfterWrites, recorder::latestSequence,
                task -> plugin.getServer().getScheduler().runTask(plugin, task));
        plugin.getServer().getServicesManager().register(EconomyJournal.class, journal, plugin, ServicePriority.Normal);
        resources.add(() -> plugin.getServer().getServicesManager().unregister(EconomyJournal.class, journal));
        return new JournalFeature(Optional.of(recorder), Optional.of(journal), resources, logger);
    }

    private static JournalSettings read(YamlFiles configFiles, Logger logger) {
        try {
            return JournalSettings.parse(configFiles.load(JournalSettings.FILE));
        } catch (ConfigException e) {
            logger.warning("The economy journal records everything, forever, until " + JournalSettings.FILE
                    + " is fixed: " + e.getMessage());
            return JournalSettings.DEFAULT;
        }
    }

    /** @return what records the journal; empty when it is off */
    public Optional<EconomyRecorder> recorder() {
        return recorder;
    }

    /** @return what reads the journal; empty when it is off */
    public Optional<JournalService> journal() {
        return journal;
    }

    /** Writes the records still queued, then closes the database connection. */
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
