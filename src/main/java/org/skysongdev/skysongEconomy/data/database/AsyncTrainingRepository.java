package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.training.TrainingRepository;
import org.skysongdev.skysongEconomy.business.training.TrainingSession;

import java.util.List;
import java.util.UUID;

/**
 * Runs another repository's writes on a {@link BackgroundWriter}; share the businesses' writer
 * so a business row is always saved before its sessions. Loading stays on the calling thread.
 */
public final class AsyncTrainingRepository implements TrainingRepository {
    private final TrainingRepository delegate;
    private final BackgroundWriter writer;

    public AsyncTrainingRepository(TrainingRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<TrainingSession> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(TrainingSession session) {
        writer.run("save training session " + session.id(), () -> delegate.save(session));
    }

    @Override
    public void delete(UUID sessionId) {
        writer.run("delete training session " + sessionId, () -> delegate.delete(sessionId));
    }
}
