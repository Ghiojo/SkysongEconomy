package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.vault.VaultRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Saves vault balances in the {@code business_vaults} table, one row per business. It blocks on
 * the database, so wrap it in {@link AsyncVaultRepository} on the server.
 */
public final class SqlVaultRepository implements VaultRepository {
    private final ConnectionProvider connections;

    /** @param connections the businesses' connection, since they are written on the same thread */
    public SqlVaultRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public Map<String, BigDecimal> loadAll() {
        try {
            Connection connection = connections.getConnection();
            Map<String, BigDecimal> balances = new HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM business_vaults");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    balances.put(rows.getString("business_id"), rows.getBigDecimal("balance").stripTrailingZeros());
                }
            }
            return balances;
        } catch (SQLException e) {
            throw new StorageException("Failed to load business vaults", e);
        }
    }

    @Override
    public void save(String businessId, BigDecimal balance) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO business_vaults (business_id, balance) VALUES (?, ?) "
                        + "ON DUPLICATE KEY UPDATE balance = VALUES(balance)")) {
            statement.setString(1, businessId);
            statement.setBigDecimal(2, balance);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the vault of business " + businessId, e);
        }
    }
}
