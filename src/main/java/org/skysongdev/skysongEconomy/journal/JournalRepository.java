package org.skysongdev.skysongEconomy.journal;

import org.skysongdev.skysongEconomy.api.journal.EconomyRecord;
import org.skysongdev.skysongEconomy.api.journal.JournalQuery;

import java.time.Instant;
import java.util.List;

/** Where journal records are kept. */
public interface JournalRepository {
    /** @return the highest sequence saved; 0 when there are no records */
    long lastSequence();

    /** Adds a record. Records are never changed once saved. */
    void save(EconomyRecord record);

    /** Adds records, in order; storage may write them together. */
    default void saveAll(List<EconomyRecord> records) {
        records.forEach(this::save);
    }

    /**
     * Reads the records that match, in sequence order (newest first when asked), skipping the query's
     * offset and stopping at its limit. Slow: never call it on the main thread.
     */
    List<EconomyRecord> query(JournalQuery query);

    /**
     * Deletes the records from before that time, for {@code keep-days}.
     *
     * @return how many were deleted
     */
    int deleteBefore(Instant time);
}
