package org.skysongdev.skysongEconomy.data.database;

import com.google.gson.Gson;
import org.skysongdev.skysongEconomy.api.claims.ClaimSale;
import org.skysongdev.skysongEconomy.claims.ClaimSaleRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Records Claim sales in the {@code claim_sales} table. */
public final class SqlClaimSaleRepository implements ClaimSaleRepository {
    private static final Gson GSON = new Gson();

    private final ConnectionProvider connections;

    public SqlClaimSaleRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public long lastId() {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "SELECT COALESCE(MAX(id), 0) FROM claim_sales");
             ResultSet rows = statement.executeQuery()) {
            return rows.next() ? rows.getLong(1) : 0;
        } catch (SQLException e) {
            throw new StorageException("Failed to read the last Claim sale", e);
        }
    }

    @Override
    public void save(ClaimSale sale) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO claim_sales (id, claim_id, claim_type, buyer_uuid, buyer_profile, asking_price, "
                        + "modifier_total, paid, modifiers, seller, seller_paid_later, sold_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, sale.id());
            statement.setString(2, sale.claimId());
            statement.setString(3, sale.claimTypeId());
            statement.setString(4, sale.buyer().player().toString());
            statement.setString(5, sale.buyer().profile());
            statement.setBigDecimal(6, sale.askingPrice());
            statement.setBigDecimal(7, sale.modifierTotal());
            statement.setBigDecimal(8, sale.paid());
            statement.setString(9, GSON.toJson(sale.modifiers()));
            statement.setString(10, sale.seller().map(GSON::toJson).orElse(null));
            statement.setBoolean(11, sale.sellerPaidLater());
            statement.setLong(12, sale.at().toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to record the sale of the Claim " + sale.claimId(), e);
        }
    }
}
