package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.module.ModulePickRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves module picks in the {@code business_modules} table, one row per pick. Deleting a
 * business deletes its rows. It blocks on the database, so wrap it in
 * {@link AsyncModulePickRepository} on the server.
 */
public final class SqlModulePickRepository implements ModulePickRepository {
    private final ConnectionProvider connections;

    public SqlModulePickRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<ModulePick> loadAll() {
        try {
            Connection connection = connections.getConnection();
            List<ModulePick> picks = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM business_modules ORDER BY business_id, pick_order");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    picks.add(new ModulePick(rows.getString("business_id"), rows.getInt("pick_order"),
                            rows.getString("skill"), rows.getString("module_id"),
                            Instant.ofEpochMilli(rows.getLong("picked_at"))));
                }
            }
            return picks;
        } catch (SQLException e) {
            throw new StorageException("Failed to load module picks", e);
        }
    }

    @Override
    public void save(ModulePick pick) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO business_modules (business_id, pick_order, skill, module_id, picked_at) VALUES (?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE skill = VALUES(skill), module_id = VALUES(module_id), "
                        + "picked_at = VALUES(picked_at)")) {
            statement.setString(1, pick.businessId());
            statement.setInt(2, pick.order());
            statement.setString(3, pick.skill());
            statement.setString(4, pick.moduleId());
            statement.setLong(5, pick.pickedAt().toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the module " + pick.moduleId() + " of " + pick.businessId(), e);
        }
    }

    @Override
    public void delete(String businessId, int order) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM business_modules WHERE business_id = ? AND pick_order = ?")) {
            statement.setString(1, businessId);
            statement.setInt(2, order);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete module pick " + order + " of " + businessId, e);
        }
    }
}
