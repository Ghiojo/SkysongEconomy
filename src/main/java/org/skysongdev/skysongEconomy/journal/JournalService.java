package org.skysongdev.skysongEconomy.journal;

import org.skysongdev.skysongEconomy.api.journal.EconomyJournal;
import org.skysongdev.skysongEconomy.api.journal.EconomyRecord;
import org.skysongdev.skysongEconomy.api.journal.JournalQuery;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * The {@link EconomyJournal} other plugins load: reads run away from the main thread, behind the
 * records still waiting to be written, and their results come back on the main thread.
 */
public final class JournalService implements EconomyJournal {
    private final Function<JournalQuery, CompletableFuture<List<EconomyRecord>>> reader;
    private final LongSupplier latestSequence;
    private final Executor mainThread;

    /**
     * @param reader         reads records after the queued writes, off the main thread
     * @param latestSequence the newest record's sequence, such as {@link EconomyRecorder#latestSequence()}
     * @param mainThread     runs a task on the server's main thread
     */
    public JournalService(Function<JournalQuery, CompletableFuture<List<EconomyRecord>>> reader,
                          LongSupplier latestSequence, Executor mainThread) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.latestSequence = Objects.requireNonNull(latestSequence, "latestSequence");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    @Override
    public CompletableFuture<List<EconomyRecord>> query(JournalQuery query) {
        Objects.requireNonNull(query, "query");
        CompletableFuture<List<EconomyRecord>> result = new CompletableFuture<>();
        reader.apply(query).whenComplete((records, failure) -> mainThread.execute(() -> {
            if (failure != null) {
                result.completeExceptionally(failure);
            } else {
                result.complete(List.copyOf(records));
            }
        }));
        return result;
    }

    @Override
    public long latestSequence() {
        return latestSequence.getAsLong();
    }
}
