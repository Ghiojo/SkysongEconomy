package org.skysongdev.skysongEconomy.data.database;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One background thread that runs database writes in the order they were queued, so saving
 * never stalls the server. Repositories whose rows depend on each other (a business and its
 * access groups) share one writer, so a child row is never written before its parent.
 */
public final class BackgroundWriter implements AutoCloseable {
    private static final long SHUTDOWN_WAIT_SECONDS = 10;

    private final Logger logger;
    private final ExecutorService thread = Executors.newSingleThreadExecutor(runnable -> {
        Thread writer = new Thread(runnable, "SkysongEconomy-business-writer");
        writer.setDaemon(true);
        return writer;
    });

    public BackgroundWriter(Logger logger) {
        this.logger = logger;
    }

    /**
     * Queues a write. A failure is logged, not thrown, because the caller has moved on.
     *
     * @param what what the write does, for the log, e.g. {@code "save business smithy"}
     */
    public void run(String what, Runnable write) {
        thread.execute(() -> {
            try {
                write.run();
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Failed to " + what, e);
            }
        });
    }

    /** Finishes the queued writes, waiting at most a few seconds. */
    @Override
    public void close() {
        thread.shutdown();
        try {
            if (!thread.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                logger.severe("Some business changes were not saved before shutdown");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
