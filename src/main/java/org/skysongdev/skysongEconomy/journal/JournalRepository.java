package org.skysongdev.skysongEconomy.journal;

import org.skysongdev.skysongEconomy.api.journal.EconomyRecord;

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
     * Deletes the records from before that time, for {@code keep-days}.
     *
     * @return how many were deleted
     */
    int deleteBefore(Instant time);
}
