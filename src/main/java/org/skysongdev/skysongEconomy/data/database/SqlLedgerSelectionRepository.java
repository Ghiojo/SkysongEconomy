package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.trade.LedgerSelectionRepository;
import org.skysongdev.skysongEconomy.trade.SelectedLedger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves selected ledgers in the {@code business_ledgers} table, one row per business and
 * ledger. It blocks on the database, so wrap it in {@link AsyncLedgerSelectionRepository} on the server.
 */
public final class SqlLedgerSelectionRepository implements LedgerSelectionRepository {
    private final ConnectionProvider connections;

    /** @param connections the businesses' connection, since they are written on the same thread */
    public SqlLedgerSelectionRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<SelectedLedger> loadAll() {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "SELECT * FROM business_ledgers ORDER BY selected_at");
             ResultSet rows = statement.executeQuery()) {
            List<SelectedLedger> selections = new ArrayList<>();
            while (rows.next()) {
                selections.add(new SelectedLedger(rows.getString("business_id"), rows.getString("ledger_id"),
                        Instant.ofEpochMilli(rows.getLong("selected_at"))));
            }
            return selections;
        } catch (SQLException e) {
            throw new StorageException("Failed to load selected ledgers", e);
        }
    }

    @Override
    public void save(SelectedLedger selection) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO business_ledgers (business_id, ledger_id, selected_at) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE selected_at = VALUES(selected_at)")) {
            statement.setString(1, selection.businessId());
            statement.setString(2, selection.ledgerId());
            statement.setLong(3, selection.selectedAt().toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save ledger " + selection.ledgerId() + " of " + selection.businessId(), e);
        }
    }

    @Override
    public void delete(String businessId, String ledgerId) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM business_ledgers WHERE business_id = ? AND ledger_id = ?")) {
            statement.setString(1, businessId);
            statement.setString(2, ledgerId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to remove ledger " + ledgerId + " of " + businessId, e);
        }
    }
}
