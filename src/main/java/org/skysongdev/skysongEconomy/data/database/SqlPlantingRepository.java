package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.items.StoredItems;
import org.skysongdev.skysongEconomy.nodes.farm.Planting;
import org.skysongdev.skysongEconomy.nodes.farm.PlantingRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Saves plantings in the {@code node_plantings} table, one row per farm node, with the primer
 * items as one blob (see {@link StoredItems#toBytes()}). It blocks on the database, so wrap it in
 * {@link AsyncPlantingRepository} on the server. A row that can't be read is skipped with a
 * warning, so one bad row doesn't stop the rest loading.
 */
public final class SqlPlantingRepository implements PlantingRepository {
    private final ConnectionProvider connections;
    private final Logger logger;

    public SqlPlantingRepository(ConnectionProvider connections, Logger logger) {
        this.connections = connections;
        this.logger = logger;
    }

    @Override
    public List<Planting> loadAll() {
        List<Planting> plantings = new ArrayList<>();
        try (PreparedStatement statement = connections.getConnection().prepareStatement("SELECT * FROM node_plantings");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                String nodeId = rows.getString("node_id");
                try {
                    plantings.add(new Planting(nodeId, rows.getString("resource_id"), rows.getString("farm_type"),
                            rows.getString("business_id"),
                            new CharacterRef(UUID.fromString(rows.getString("player_uuid")), rows.getString("profile")),
                            StoredItems.fromBytes(rows.getBytes("items"))));
                } catch (IllegalArgumentException e) {
                    logger.warning("Skipped the planting in node " + nodeId + ", which can't be read: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to load node plantings", e);
        }
        return plantings;
    }

    @Override
    public void save(Planting planting) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO node_plantings (node_id, resource_id, farm_type, business_id, player_uuid, profile, items) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                        + "resource_id = VALUES(resource_id), farm_type = VALUES(farm_type), "
                        + "business_id = VALUES(business_id), player_uuid = VALUES(player_uuid), "
                        + "profile = VALUES(profile), items = VALUES(items)")) {
            statement.setString(1, planting.nodeId());
            statement.setString(2, planting.resourceId());
            statement.setString(3, planting.farmType());
            statement.setString(4, planting.businessId());
            statement.setString(5, planting.planter().player().toString());
            statement.setString(6, planting.planter().profile());
            statement.setBytes(7, planting.primer().toBytes());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the planting in node " + planting.nodeId(), e);
        }
    }

    @Override
    public void delete(String nodeId) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM node_plantings WHERE node_id = ?")) {
            statement.setString(1, nodeId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the planting in node " + nodeId, e);
        }
    }
}
