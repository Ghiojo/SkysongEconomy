package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.prices.BaseValueRepository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * The price database ({@code price_ledger} table): each Skysong item's base value expression,
 * keyed by its material id. {@code cost} keeps the value at the time of the last change, for
 * tools that read the table directly; rows from before expressions existed have only a
 * {@code cost}, which is read as a plain number.
 */
public final class PriceLedgerRepository implements BaseValueRepository {
    private final ConnectionProvider connections;

    public PriceLedgerRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public Map<String, String> loadAll() {
        Map<String, String> entries = new HashMap<>();
        try (PreparedStatement statement = connections.getConnection()
                .prepareStatement("SELECT materialId, cost, expression FROM price_ledger");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                String expression = rows.getString("expression");
                if (expression == null || expression.isBlank()) {
                    expression = BigDecimal.valueOf(rows.getFloat("cost")).stripTrailingZeros().toPlainString();
                }
                entries.put(rows.getString("materialId"), expression);
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to load the price database", e);
        }
        return entries;
    }

    @Override
    public void save(String materialId, String expression, BigDecimal valueNow) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO price_ledger (materialId, cost, expression) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE cost = VALUES(cost), expression = VALUES(expression)")) {
            statement.setString(1, materialId);
            statement.setFloat(2, valueNow.floatValue());
            statement.setString(3, expression);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the base value of " + materialId, e);
        }
    }

    @Override
    public void delete(String materialId) {
        try (PreparedStatement statement = connections.getConnection()
                .prepareStatement("DELETE FROM price_ledger WHERE materialId = ?")) {
            statement.setString(1, materialId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to remove the base value of " + materialId, e);
        }
    }
}
