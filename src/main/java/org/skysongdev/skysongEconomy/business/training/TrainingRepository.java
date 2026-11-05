package org.skysongdev.skysongEconomy.business.training;

import java.util.List;
import java.util.UUID;

/** Where running training sessions are saved, so a restart keeps their progress. */
public interface TrainingRepository {
    List<TrainingSession> loadAll();

    /** Saves the session, replacing the earlier copy with the same id. */
    void save(TrainingSession session);

    /** Deletes a session that ended. */
    void delete(UUID sessionId);
}
