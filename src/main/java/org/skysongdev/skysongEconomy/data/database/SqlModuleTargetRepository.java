package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.module.npc.ModuleTargetRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Saves the NPCs chosen for module benefits in the {@code business_module_targets} table, one
 * row per chosen NPC. Deleting a business deletes its rows. It blocks on the database, so wrap
 * it in {@link AsyncModuleTargetRepository} on the server.
 */
public final class SqlModuleTargetRepository implements ModuleTargetRepository {
    private final ConnectionProvider connections;
    private final Logger logger;

    /** @param logger told about rows that can't be read, which are skipped */
    public SqlModuleTargetRepository(ConnectionProvider connections, Logger logger) {
        this.connections = connections;
        this.logger = logger;
    }

    @Override
    public List<Row> loadAll() {
        try {
            Connection connection = connections.getConnection();
            List<Row> rows = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM business_module_targets ORDER BY chosen_at, npc_uuid");
                 ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    String npc = results.getString("npc_uuid");
                    try {
                        rows.add(new Row(results.getString("business_id"), results.getInt("pick_order"),
                                results.getInt("effect"), UUID.fromString(npc)));
                    } catch (IllegalArgumentException e) {
                        logger.warning("Skipped a module choice for the NPC " + npc + ", which is not an NPC id");
                    }
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new StorageException("Failed to load the NPCs chosen for module benefits", e);
        }
    }

    @Override
    public void save(Row row) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT IGNORE INTO business_module_targets (business_id, pick_order, effect, npc_uuid, chosen_at) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            statement.setString(1, row.businessId());
            statement.setInt(2, row.pickOrder());
            statement.setInt(3, row.effect());
            statement.setString(4, row.npc().toString());
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the NPC " + row.npc() + " chosen for a module of " + row.businessId(), e);
        }
    }

    @Override
    public void deletePick(String businessId, int pickOrder) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM business_module_targets WHERE business_id = ? AND pick_order = ?")) {
            statement.setString(1, businessId);
            statement.setInt(2, pickOrder);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the choices of module pick " + pickOrder + " of " + businessId, e);
        }
    }

    @Override
    public void deleteNpc(UUID npc) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM business_module_targets WHERE npc_uuid = ?")) {
            statement.setString(1, npc.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the module choices of the NPC " + npc, e);
        }
    }
}
