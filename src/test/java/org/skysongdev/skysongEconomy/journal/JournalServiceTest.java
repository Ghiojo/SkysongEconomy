package org.skysongdev.skysongEconomy.journal;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.journal.EconomyRecord;
import org.skysongdev.skysongEconomy.api.journal.JournalQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalServiceTest {
    private final List<Runnable> mainThread = new ArrayList<>();

    private void runMainThread() {
        List.copyOf(mainThread).forEach(Runnable::run);
        mainThread.clear();
    }

    @Test
    void resultsArriveOnTheMainThread() {
        CompletableFuture<List<EconomyRecord>> read = new CompletableFuture<>();
        JournalService service = new JournalService(query -> read, () -> 42, mainThread::add);

        CompletableFuture<List<EconomyRecord>> result = service.query(JournalQuery.builder().build());
        read.complete(List.of());
        assertFalse(result.isDone(), "not until the main thread runs");
        runMainThread();

        assertEquals(List.of(), result.join());
        assertEquals(42, service.latestSequence());
    }

    @Test
    void aFailedReadFailsTheResultOnTheMainThread() {
        JournalService service = new JournalService(
                query -> CompletableFuture.failedFuture(new IllegalStateException("no database")), () -> 0,
                mainThread::add);

        CompletableFuture<List<EconomyRecord>> result = service.query(JournalQuery.builder().build());
        assertFalse(result.isDone());
        runMainThread();

        assertTrue(result.isCompletedExceptionally());
        ExecutionException failure = assertThrows(ExecutionException.class, result::get);
        assertInstanceOf(IllegalStateException.class, failure.getCause());
    }
}
