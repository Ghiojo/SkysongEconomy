package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.training.TrainingRepository;
import org.skysongdev.skysongEconomy.business.training.TrainingSession;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Saves running training sessions in the {@code business_training} table. Times are epoch
 * milliseconds, durations seconds. It blocks on the database, so wrap it in
 * {@link AsyncTrainingRepository} on the server.
 */
public final class SqlTrainingRepository implements TrainingRepository {
    private final ConnectionProvider connections;

    /** @param connections the businesses' connection, since they are written on the same thread */
    public SqlTrainingRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<TrainingSession> loadAll() {
        try {
            Connection connection = connections.getConnection();
            List<TrainingSession> sessions = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM business_training ORDER BY started_at");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    sessions.add(new TrainingSession(UUID.fromString(rows.getString("id")), rows.getString("business_id"),
                            new CharacterRef(UUID.fromString(rows.getString("player_uuid")), rows.getString("profile")),
                            Instant.ofEpochMilli(rows.getLong("started_at")),
                            Duration.ofSeconds(rows.getLong("required_seconds")),
                            Duration.ofSeconds(rows.getLong("worked_seconds")),
                            rows.getBigDecimal("price").stripTrailingZeros()));
                }
            }
            return sessions;
        } catch (SQLException e) {
            throw new StorageException("Failed to load training sessions", e);
        }
    }

    @Override
    public void save(TrainingSession session) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO business_training (id, business_id, player_uuid, profile, started_at, required_seconds, "
                        + "worked_seconds, price) VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE worked_seconds = VALUES(worked_seconds)")) {
            statement.setString(1, session.id().toString());
            statement.setString(2, session.businessId());
            statement.setString(3, session.character().player().toString());
            statement.setString(4, session.character().profile());
            statement.setLong(5, session.startedAt().toEpochMilli());
            statement.setLong(6, session.required().toSeconds());
            statement.setLong(7, session.worked().toSeconds());
            statement.setBigDecimal(8, session.price());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save training session " + session.id(), e);
        }
    }

    @Override
    public void delete(UUID sessionId) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM business_training WHERE id = ?")) {
            statement.setString(1, sessionId.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete training session " + sessionId, e);
        }
    }
}
