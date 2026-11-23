package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.claims.ClaimPrice;
import org.skysongdev.skysongEconomy.claims.ClaimPriceRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves Claims' asking prices in the {@code claim_prices} table, one row per Claim. It blocks on
 * the database, so wrap it in {@link AsyncClaimPriceRepository} on the server.
 */
public final class SqlClaimPriceRepository implements ClaimPriceRepository {
    private final ConnectionProvider connections;

    public SqlClaimPriceRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<Row> loadAll() {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "SELECT claim_id, expression, set_by, set_at FROM claim_prices");
             ResultSet rows = statement.executeQuery()) {
            List<Row> prices = new ArrayList<>();
            while (rows.next()) {
                prices.add(new Row(rows.getString("claim_id"), rows.getString("expression"), rows.getString("set_by"),
                        rows.getLong("set_at")));
            }
            return prices;
        } catch (SQLException e) {
            throw new StorageException("Failed to load Claim prices", e);
        }
    }

    @Override
    public void save(ClaimPrice price) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "REPLACE INTO claim_prices (claim_id, expression, set_by, set_at) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, price.claimId());
            statement.setString(2, price.expression().text());
            statement.setString(3, price.setBy());
            statement.setLong(4, price.setAt().toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the price of the Claim " + price.claimId(), e);
        }
    }

    @Override
    public void delete(String claimId) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM claim_prices WHERE claim_id = ?")) {
            statement.setString(1, claimId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the price of the Claim " + claimId, e);
        }
    }
}
