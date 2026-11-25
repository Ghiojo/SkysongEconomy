package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.api.journal.EconomyRecord;
import org.skysongdev.skysongEconomy.journal.JournalRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Saves journal records on the background writer, so recording never waits for the database. Records
 * made while a write is queued are written together with it, in one batch; whatever is queued when
 * the plugin stops is written before the writer closes.
 */
public final class AsyncJournalRepository implements JournalRepository {
    private final JournalRepository delegate;
    private final BackgroundWriter writer;
    private final Queue<EconomyRecord> waiting = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean drainQueued = new AtomicBoolean();

    public AsyncJournalRepository(JournalRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public long lastSequence() {
        return delegate.lastSequence();
    }

    @Override
    public void save(EconomyRecord record) {
        waiting.add(record);
        if (drainQueued.compareAndSet(false, true)) {
            writer.run("save economy journal records", this::drain);
        }
    }

    @Override
    public int deleteBefore(Instant time) {
        writer.run("delete economy journal records from before " + time, () -> delegate.deleteBefore(time));
        return 0;
    }

    private void drain() {
        // Cleared first, so a record added from now on queues another drain instead of being missed.
        drainQueued.set(false);
        List<EconomyRecord> batch = new ArrayList<>();
        for (EconomyRecord record = waiting.poll(); record != null; record = waiting.poll()) {
            batch.add(record);
        }
        if (!batch.isEmpty()) {
            delegate.saveAll(batch);
        }
    }
}
