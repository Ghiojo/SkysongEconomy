package org.skysongdev.skysongEconomy.api.journal;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Reads the economy journal. Registered with Bukkit's services manager while the journal is on:
 *
 * <pre>{@code
 * EconomyJournal journal = Bukkit.getServicesManager().load(EconomyJournal.class); // null when it is off
 * journal.query(JournalQuery.builder().afterSequence(lastSeen).build())
 *         .thenAccept(records -> records.forEach(this::keep));
 * }</pre>
 *
 * Call it from the main thread.
 */
public interface EconomyJournal {
    /**
     * Reads the records that match, from the database, away from the main thread. Records made just
     * before the call are included, even when they were not written yet.
     *
     * @return the records, in sequence order (newest first when the query asks), completed on the main
     *         thread; completed exceptionally when the database can't be read
     */
    CompletableFuture<List<EconomyRecord>> query(JournalQuery query);

    /** @return the sequence of the newest record; 0 when there are none */
    long latestSequence();
}
