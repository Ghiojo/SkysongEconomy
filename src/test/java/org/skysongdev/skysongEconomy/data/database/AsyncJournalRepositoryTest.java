package org.skysongdev.skysongEconomy.data.database;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.journal.EconomyRecord;
import org.skysongdev.skysongEconomy.api.journal.RecordKind;
import org.skysongdev.skysongEconomy.journal.JournalRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncJournalRepositoryTest {
    private static EconomyRecord record(long sequence) {
        return new EconomyRecord(sequence, UUID.randomUUID(), RecordKind.HARVEST, "node", Instant.EPOCH,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), List.of(), List.of(), Map.of());
    }

    @Test
    void everyRecordIsWrittenInOrderInBatchesBeforeTheWriterCloses() {
        Batches batches = new Batches();
        BackgroundWriter writer = new BackgroundWriter(Logger.getAnonymousLogger());
        AsyncJournalRepository repository = new AsyncJournalRepository(batches, writer);

        LongStream.rangeClosed(1, 500).forEach(sequence -> repository.save(record(sequence)));
        writer.close();

        List<Long> written = batches.batches.stream().flatMap(List::stream).map(EconomyRecord::sequence).toList();
        assertEquals(LongStream.rangeClosed(1, 500).boxed().toList(), written);
        assertEquals(7, repository.lastSequence());
    }

    private static final class Batches implements JournalRepository {
        final List<List<EconomyRecord>> batches = new CopyOnWriteArrayList<>();

        @Override
        public long lastSequence() {
            return 7;
        }

        @Override
        public void save(EconomyRecord record) {
            saveAll(List.of(record));
        }

        @Override
        public void saveAll(List<EconomyRecord> records) {
            batches.add(List.copyOf(records));
        }

        @Override
        public int deleteBefore(Instant time) {
            return 0;
        }
    }
}
