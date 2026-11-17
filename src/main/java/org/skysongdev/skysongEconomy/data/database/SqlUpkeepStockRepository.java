package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.items.StoredItems;
import org.skysongdev.skysongEconomy.upkeep.UpkeepStockRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Saves the items stocked in NPCs for their upkeep in the {@code npc_upkeep_stock} table, one row
 * per NPC that has any. It blocks on the database, so wrap it in
 * {@link AsyncUpkeepStockRepository} on the server.
 */
public final class SqlUpkeepStockRepository implements UpkeepStockRepository {
    private final ConnectionProvider connections;
    private final Logger logger;

    /** @param logger told about rows that can't be read, which are skipped */
    public SqlUpkeepStockRepository(ConnectionProvider connections, Logger logger) {
        this.connections = connections;
        this.logger = logger;
    }

    @Override
    public Map<UUID, StoredItems> loadAll() {
        try {
            Connection connection = connections.getConnection();
            Map<UUID, StoredItems> rows = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM npc_upkeep_stock");
                 ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    String npc = results.getString("npc_uuid");
                    try {
                        rows.put(UUID.fromString(npc), StoredItems.fromBytes(results.getBytes("items")));
                    } catch (IllegalArgumentException e) {
                        logger.warning("Skipped the upkeep stock of the NPC " + npc + ", which can't be read: " + e.getMessage());
                    }
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new StorageException("Failed to load NPC upkeep stocks", e);
        }
    }

    @Override
    public void save(UUID npc, StoredItems items) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "REPLACE INTO npc_upkeep_stock (npc_uuid, items) VALUES (?, ?)")) {
            statement.setString(1, npc.toString());
            statement.setBytes(2, items.toBytes());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the upkeep stock of the NPC " + npc, e);
        }
    }

    @Override
    public void delete(UUID npc) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM npc_upkeep_stock WHERE npc_uuid = ?")) {
            statement.setString(1, npc.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the upkeep stock of the NPC " + npc, e);
        }
    }
}
