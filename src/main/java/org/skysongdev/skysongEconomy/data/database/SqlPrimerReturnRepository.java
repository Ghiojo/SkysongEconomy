package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.items.StoredItems;
import org.skysongdev.skysongEconomy.nodes.farm.PrimerReturn;
import org.skysongdev.skysongEconomy.nodes.farm.PrimerReturnRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Saves primers waiting for an offline planter in the {@code node_primer_returns} table. It
 * blocks on the database, so wrap it in {@link AsyncPrimerReturnRepository} on the server. A row
 * that can't be read is skipped with a warning.
 */
public final class SqlPrimerReturnRepository implements PrimerReturnRepository {
    private final ConnectionProvider connections;
    private final Logger logger;

    public SqlPrimerReturnRepository(ConnectionProvider connections, Logger logger) {
        this.connections = connections;
        this.logger = logger;
    }

    @Override
    public List<PrimerReturn> loadAll() {
        List<PrimerReturn> returns = new ArrayList<>();
        try (PreparedStatement statement = connections.getConnection().prepareStatement("SELECT * FROM node_primer_returns");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                String id = rows.getString("id");
                try {
                    returns.add(new PrimerReturn(UUID.fromString(id), UUID.fromString(rows.getString("player_uuid")),
                            StoredItems.fromBytes(rows.getBytes("items"))));
                } catch (IllegalArgumentException e) {
                    logger.warning("Skipped the waiting primer " + id + ", which can't be read: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to load waiting primers", e);
        }
        return returns;
    }

    @Override
    public void save(PrimerReturn waiting) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO node_primer_returns (id, player_uuid, items) VALUES (?, ?, ?)")) {
            statement.setString(1, waiting.id().toString());
            statement.setString(2, waiting.player().toString());
            statement.setBytes(3, waiting.items().toBytes());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the waiting primer " + waiting.id(), e);
        }
    }

    @Override
    public void delete(UUID id) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM node_primer_returns WHERE id = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the waiting primer " + id, e);
        }
    }
}
