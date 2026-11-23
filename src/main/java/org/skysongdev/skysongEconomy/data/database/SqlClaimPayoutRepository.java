package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.claims.ClaimPayoutRepository;
import org.skysongdev.skysongEconomy.claims.PendingPayout;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Keeps sellers' unpaid shares in the {@code claim_payouts} table. */
public final class SqlClaimPayoutRepository implements ClaimPayoutRepository {
    private final ConnectionProvider connections;

    public SqlClaimPayoutRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<PendingPayout> loadAll() {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "SELECT sale_id, account_uuid, claim_id, amount, sold_at FROM claim_payouts ORDER BY sold_at");
             ResultSet rows = statement.executeQuery()) {
            List<PendingPayout> payouts = new ArrayList<>();
            while (rows.next()) {
                payouts.add(new PendingPayout(rows.getLong("sale_id"), UUID.fromString(rows.getString("account_uuid")),
                        rows.getString("claim_id"), rows.getBigDecimal("amount"), Instant.ofEpochMilli(rows.getLong("sold_at"))));
            }
            return payouts;
        } catch (SQLException e) {
            throw new StorageException("Failed to load Claim payouts", e);
        }
    }

    @Override
    public void save(PendingPayout payout) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "REPLACE INTO claim_payouts (sale_id, account_uuid, claim_id, amount, sold_at) VALUES (?, ?, ?, ?, ?)")) {
            statement.setLong(1, payout.saleId());
            statement.setString(2, payout.account().toString());
            statement.setString(3, payout.claimId());
            statement.setBigDecimal(4, payout.amount());
            statement.setLong(5, payout.soldAt().toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save a payout for the Claim " + payout.claimId(), e);
        }
    }

    @Override
    public void delete(long saleId, UUID account) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM claim_payouts WHERE sale_id = ? AND account_uuid = ?")) {
            statement.setLong(1, saleId);
            statement.setString(2, account.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the payout of sale " + saleId, e);
        }
    }
}
