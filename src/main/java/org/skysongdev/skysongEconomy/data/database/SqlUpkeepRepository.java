package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepRepository;
import org.skysongdev.skysongEconomy.upkeep.UpkeepState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Saves each NPC's upkeep in the {@code npc_upkeep} table, one row per NPC. It blocks on the
 * database, so wrap it in {@link AsyncUpkeepRepository} on the server.
 */
public final class SqlUpkeepRepository implements UpkeepRepository {
    private final ConnectionProvider connections;
    private final Logger logger;

    /** @param logger told about rows that can't be read, which are skipped */
    public SqlUpkeepRepository(ConnectionProvider connections, Logger logger) {
        this.connections = connections;
        this.logger = logger;
    }

    @Override
    public List<NpcUpkeep> loadAll() {
        try {
            Connection connection = connections.getConnection();
            List<NpcUpkeep> rows = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM npc_upkeep");
                 ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    String npc = results.getString("npc_uuid");
                    try {
                        rows.add(new NpcUpkeep(UUID.fromString(npc), results.getString("profile"),
                                results.getBoolean("set_by_hand"), UpkeepState.valueOf(results.getString("state")),
                                Instant.ofEpochMilli(results.getLong("next_due")),
                                Instant.ofEpochMilli(results.getLong("changed_at"))));
                    } catch (IllegalArgumentException e) {
                        logger.warning("Skipped the upkeep of the NPC " + npc + ", which can't be read: " + e.getMessage());
                    }
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new StorageException("Failed to load NPC upkeeps", e);
        }
    }

    @Override
    public void save(NpcUpkeep upkeep) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "REPLACE INTO npc_upkeep (npc_uuid, profile, set_by_hand, state, next_due, changed_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, upkeep.npc().toString());
            if (upkeep.profile() == null) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, upkeep.profile());
            }
            statement.setBoolean(3, upkeep.setByHand());
            statement.setString(4, upkeep.state().name());
            statement.setLong(5, upkeep.nextDue().toEpochMilli());
            statement.setLong(6, upkeep.changed().toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the upkeep of the NPC " + upkeep.npc(), e);
        }
    }

    @Override
    public void delete(UUID npc) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM npc_upkeep WHERE npc_uuid = ?")) {
            statement.setString(1, npc.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the upkeep of the NPC " + npc, e);
        }
    }
}
