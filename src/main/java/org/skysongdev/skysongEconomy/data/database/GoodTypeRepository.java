package org.skysongdev.skysongEconomy.data.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Which good types (tags such as "weapon" or "food") each material ID belongs to ({@code good_types} table). */
public final class GoodTypeRepository {
    private final ConnectionProvider connections;

    public GoodTypeRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    /** @return false if the material already had this good type */
    public boolean add(String materialId, String goodType) throws SQLException {
        if (has(materialId, goodType)) {
            return false;
        }
        try (PreparedStatement statement = connections.getConnection()
                .prepareStatement("INSERT INTO good_types (goodType, materialId) VALUES (?, ?)")) {
            statement.setString(1, goodType);
            statement.setString(2, materialId);
            statement.executeUpdate();
        }
        return true;
    }

    /** @return whether the material had this good type */
    public boolean remove(String materialId, String goodType) throws SQLException {
        try (PreparedStatement statement = connections.getConnection()
                .prepareStatement("DELETE FROM good_types WHERE goodType = ? AND materialId = ?")) {
            statement.setString(1, goodType);
            statement.setString(2, materialId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean has(String materialId, String goodType) throws SQLException {
        try (PreparedStatement statement = connections.getConnection()
                .prepareStatement("SELECT 1 FROM good_types WHERE materialId = ? AND goodType = ? LIMIT 1")) {
            statement.setString(1, materialId);
            statement.setString(2, goodType);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    public Set<String> findGoodTypes(String materialId) throws SQLException {
        Set<String> goodTypes = new LinkedHashSet<>();
        try (PreparedStatement statement = connections.getConnection()
                .prepareStatement("SELECT goodType FROM good_types WHERE materialId = ?")) {
            statement.setString(1, materialId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    goodTypes.add(results.getString("goodType"));
                }
            }
        }
        return goodTypes;
    }

    /** @return every item's good types, by material id */
    public Map<String, Set<String>> loadAll() throws SQLException {
        Map<String, Set<String>> byItem = new HashMap<>();
        try (PreparedStatement statement = connections.getConnection()
                .prepareStatement("SELECT goodType, materialId FROM good_types");
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                byItem.computeIfAbsent(results.getString("materialId"), id -> new LinkedHashSet<>())
                        .add(results.getString("goodType"));
            }
        }
        return byItem;
    }

    /** Every good type that at least one of the given materials belongs to, e.g. all items of an import offer. */
    public Set<String> findGoodTypes(Collection<String> materialIds) throws SQLException {
        Set<String> goodTypes = new LinkedHashSet<>();
        for (String materialId : materialIds) {
            goodTypes.addAll(findGoodTypes(materialId));
        }
        return goodTypes;
    }
}
