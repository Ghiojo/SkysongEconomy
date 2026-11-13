package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcRepository;
import org.skysongdev.skysongEconomy.business.npc.SlotSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Saves business NPCs in the {@code business_npcs} table, one row per NPC. Deleting a business
 * deletes its rows. It blocks on the database, so wrap it in {@link AsyncBusinessNpcRepository}
 * on the server.
 */
public final class SqlBusinessNpcRepository implements BusinessNpcRepository {
    private final ConnectionProvider connections;
    private final Logger logger;

    /** @param logger told about rows that can't be read, which are skipped */
    public SqlBusinessNpcRepository(ConnectionProvider connections, Logger logger) {
        this.connections = connections;
        this.logger = logger;
    }

    @Override
    public List<BusinessNpc> loadAll() {
        try {
            Connection connection = connections.getConnection();
            List<BusinessNpc> npcs = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM business_npcs ORDER BY created_at");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    read(rows).ifPresent(npcs::add);
                }
            }
            return npcs;
        } catch (SQLException e) {
            throw new StorageException("Failed to load business NPCs", e);
        }
    }

    private Optional<BusinessNpc> read(ResultSet rows) throws SQLException {
        String npcId = rows.getString("npc_uuid");
        try {
            SlotSource source = SlotSource.parse(rows.getString("source"))
                    .orElseThrow(() -> new IllegalArgumentException("its source is not written as kind:id"));
            return Optional.of(new BusinessNpc(UUID.fromString(npcId), rows.getString("business_id"), source,
                    Instant.ofEpochMilli(rows.getLong("created_at"))));
        } catch (IllegalArgumentException e) {
            logger.warning("Skipped the business NPC " + npcId + ", which can't be read: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(BusinessNpc npc) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO business_npcs (npc_uuid, business_id, source, created_at) VALUES (?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE business_id = VALUES(business_id), source = VALUES(source), "
                        + "created_at = VALUES(created_at)")) {
            statement.setString(1, npc.npc().toString());
            statement.setString(2, npc.businessId());
            statement.setString(3, npc.source().key());
            statement.setLong(4, npc.createdAt().toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the business NPC " + npc.npc(), e);
        }
    }

    @Override
    public void delete(UUID npc) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM business_npcs WHERE npc_uuid = ?")) {
            statement.setString(1, npc.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the business NPC " + npc, e);
        }
    }
}
