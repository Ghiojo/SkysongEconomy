package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.bench.BenchRepository;
import org.skysongdev.skysongEconomy.business.bench.BusinessBench;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves bench links in the {@code business_benches} table, one row per bench block. Deleting a
 * business deletes its rows. It blocks on the database, so wrap it in {@link AsyncBenchRepository}
 * on the server.
 */
public final class SqlBenchRepository implements BenchRepository {
    private final ConnectionProvider connections;

    public SqlBenchRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<BusinessBench> loadAll() {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "SELECT * FROM business_benches ORDER BY assigned_at");
             ResultSet rows = statement.executeQuery()) {
            List<BusinessBench> benches = new ArrayList<>();
            while (rows.next()) {
                benches.add(new BusinessBench(
                        new BlockPosition(rows.getString("world"), rows.getInt("x"), rows.getInt("y"), rows.getInt("z")),
                        rows.getString("business_id"), rows.getString("assigned_by"),
                        Instant.ofEpochMilli(rows.getLong("assigned_at"))));
            }
            return benches;
        } catch (SQLException e) {
            throw new StorageException("Failed to load the business benches", e);
        }
    }

    @Override
    public void save(BusinessBench bench) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO business_benches (world, x, y, z, business_id, assigned_by, assigned_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE business_id = VALUES(business_id), "
                        + "assigned_by = VALUES(assigned_by), assigned_at = VALUES(assigned_at)")) {
            setPosition(statement, bench.bench());
            statement.setString(5, bench.businessId());
            statement.setString(6, bench.assignedBy());
            statement.setLong(7, bench.assignedAt().toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the bench at " + bench.bench(), e);
        }
    }

    @Override
    public void delete(BlockPosition bench) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM business_benches WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            setPosition(statement, bench);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the bench at " + bench, e);
        }
    }

    private static void setPosition(PreparedStatement statement, BlockPosition position) throws SQLException {
        statement.setString(1, position.world());
        statement.setInt(2, position.x());
        statement.setInt(3, position.y());
        statement.setInt(4, position.z());
    }
}
