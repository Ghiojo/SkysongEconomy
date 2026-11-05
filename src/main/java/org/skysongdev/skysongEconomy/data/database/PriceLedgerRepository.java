package org.skysongdev.skysongEconomy.data.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** The price of every Skysong item, keyed by its material ID ({@code price_ledger} table). */
public final class PriceLedgerRepository {
    private final ConnectionProvider connections;

    public PriceLedgerRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    public Optional<Float> findCost(String materialId) throws SQLException {
        try (PreparedStatement statement = connections.getConnection()
                .prepareStatement("SELECT cost FROM price_ledger WHERE materialId = ?")) {
            statement.setString(1, materialId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(results.getFloat("cost")) : Optional.empty();
            }
        }
    }

    /** Creates the entry or replaces its cost. */
    public void setCost(String materialId, float cost) throws SQLException {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO price_ledger (materialId, cost) VALUES (?, ?) ON DUPLICATE KEY UPDATE cost = ?")) {
            statement.setString(1, materialId);
            statement.setFloat(2, cost);
            statement.setFloat(3, cost);
            statement.executeUpdate();
        }
    }

    /** @return whether an entry existed and was removed */
    public boolean delete(String materialId) throws SQLException {
        try (PreparedStatement statement = connections.getConnection()
                .prepareStatement("DELETE FROM price_ledger WHERE materialId = ?")) {
            statement.setString(1, materialId);
            return statement.executeUpdate() > 0;
        }
    }
}
