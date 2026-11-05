package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.trade.delivery.Delivery;
import org.skysongdev.skysongEconomy.trade.delivery.DeliveryRepository;
import org.skysongdev.skysongEconomy.trade.delivery.ItemListCodec;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Saves deliveries in the {@code business_deliveries} table until they are collected. It blocks
 * on the database, so wrap it in {@link AsyncDeliveryRepository} on the server.
 */
public final class SqlDeliveryRepository implements DeliveryRepository {
    private final ConnectionProvider connections;

    /** @param connections the businesses' connection, since they are written on the same thread */
    public SqlDeliveryRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<Delivery> loadAll() {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "SELECT * FROM business_deliveries ORDER BY ordered_at");
             ResultSet rows = statement.executeQuery()) {
            List<Delivery> deliveries = new ArrayList<>();
            while (rows.next()) {
                deliveries.add(new Delivery(rows.getString("id"), rows.getString("business_id"), rows.getString("ledger_id"),
                        rows.getString("import_id"), rows.getString("delivery_time_id"),
                        ItemListCodec.decode(rows.getString("items")),
                        new CharacterRef(UUID.fromString(rows.getString("player_uuid")), rows.getString("profile")),
                        rows.getBigDecimal("price"), Instant.ofEpochMilli(rows.getLong("ordered_at")),
                        Instant.ofEpochMilli(rows.getLong("arrives_at"))));
            }
            return deliveries;
        } catch (SQLException e) {
            throw new StorageException("Failed to load deliveries", e);
        }
    }

    @Override
    public void save(Delivery delivery) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO business_deliveries (id, business_id, ledger_id, import_id, delivery_time_id, items, "
                        + "player_uuid, profile, price, ordered_at, arrives_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, delivery.id());
            statement.setString(2, delivery.businessId());
            statement.setString(3, delivery.ledgerId());
            statement.setString(4, delivery.importId());
            statement.setString(5, delivery.deliveryTimeId());
            statement.setString(6, ItemListCodec.encode(delivery.items()));
            statement.setString(7, delivery.orderedBy().player().toString());
            statement.setString(8, delivery.orderedBy().profile());
            statement.setBigDecimal(9, delivery.price());
            statement.setLong(10, delivery.orderedAt().toEpochMilli());
            statement.setLong(11, delivery.arrivesAt().toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save delivery " + delivery.id(), e);
        }
    }

    @Override
    public void delete(String id) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM business_deliveries WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete delivery " + id, e);
        }
    }
}
