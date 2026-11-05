package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.BusinessRepository;
import org.skysongdev.skysongEconomy.business.BusinessSnapshot;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs another repository's writes on one background thread, in the order they were made, so
 * saving never stalls the server. Loading stays on the calling thread because the plugin needs
 * the data before it can start.
 */
public final class AsyncBusinessRepository implements BusinessRepository, AutoCloseable {
    private static final long SHUTDOWN_WAIT_SECONDS = 10;

    private final BusinessRepository delegate;
    private final Logger logger;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "SkysongEconomy-business-writer");
        thread.setDaemon(true);
        return thread;
    });

    public AsyncBusinessRepository(BusinessRepository delegate, Logger logger) {
        this.delegate = delegate;
        this.logger = logger;
    }

    @Override
    public List<BusinessSnapshot> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(BusinessSnapshot business) {
        run("save business " + business.id(), () -> delegate.save(business));
    }

    @Override
    public void delete(String id) {
        run("delete business " + id, () -> delegate.delete(id));
    }

    /** Finishes the queued writes, waiting at most a few seconds. */
    @Override
    public void close() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                logger.severe("Some business changes were not saved before shutdown");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void run(String what, Runnable write) {
        writer.execute(() -> {
            try {
                write.run();
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Failed to " + what, e);
            }
        });
    }
}
