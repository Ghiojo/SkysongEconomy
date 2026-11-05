package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.shift.Shift;
import org.skysongdev.skysongEconomy.business.shift.ShiftPay;
import org.skysongdev.skysongEconomy.business.shift.ShiftRepository;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Saves shifts in the {@code business_shifts} table until their pay is retrieved. Times are
 * epoch milliseconds, durations seconds. It blocks on the database, so wrap it in
 * {@link AsyncShiftRepository} on the server.
 */
public final class SqlShiftRepository implements ShiftRepository {
    private final ConnectionProvider connections;

    /** @param connections the businesses' connection, since they are written on the same thread */
    public SqlShiftRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<Shift> loadAll() {
        try {
            Connection connection = connections.getConnection();
            List<Shift> shifts = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM business_shifts ORDER BY started_at");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    long finished = rows.getLong("finished_at");
                    boolean isFinished = !rows.wasNull();
                    shifts.add(new Shift(UUID.fromString(rows.getString("id")), rows.getString("business_id"),
                            new CharacterRef(UUID.fromString(rows.getString("player_uuid")), rows.getString("profile")),
                            Instant.ofEpochMilli(rows.getLong("started_at")),
                            Duration.ofSeconds(rows.getLong("required_seconds")),
                            Duration.ofSeconds(rows.getLong("worked_seconds")),
                            new ShiftPay(rows.getBigDecimal("money").stripTrailingZeros(), rows.getLong("tokens")),
                            rows.getBigDecimal("vault_cut").stripTrailingZeros(),
                            isFinished ? Instant.ofEpochMilli(finished) : null));
                }
            }
            return shifts;
        } catch (SQLException e) {
            throw new StorageException("Failed to load shifts", e);
        }
    }

    @Override
    public void save(Shift shift) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO business_shifts (id, business_id, player_uuid, profile, started_at, required_seconds, "
                        + "worked_seconds, money, tokens, vault_cut, finished_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE worked_seconds = VALUES(worked_seconds), finished_at = VALUES(finished_at)")) {
            statement.setString(1, shift.id().toString());
            statement.setString(2, shift.businessId());
            statement.setString(3, shift.character().player().toString());
            statement.setString(4, shift.character().profile());
            statement.setLong(5, shift.startedAt().toEpochMilli());
            statement.setLong(6, shift.required().toSeconds());
            statement.setLong(7, shift.worked().toSeconds());
            statement.setBigDecimal(8, shift.pay().money());
            statement.setLong(9, shift.pay().tokens());
            statement.setBigDecimal(10, shift.vaultCut());
            if (shift.isFinished()) {
                statement.setLong(11, shift.finishedAt().toEpochMilli());
            } else {
                statement.setNull(11, Types.BIGINT);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save shift " + shift.id(), e);
        }
    }

    @Override
    public void delete(UUID shiftId) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM business_shifts WHERE id = ?")) {
            statement.setString(1, shiftId.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete shift " + shiftId, e);
        }
    }
}
