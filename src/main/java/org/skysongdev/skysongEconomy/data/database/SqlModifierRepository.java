package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.prices.modifier.AppliesTo;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierRepository;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierTarget;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves price modifiers in the {@code price_modifiers} table, one row each. It blocks on the
 * database, so wrap it in {@link AsyncModifierRepository} on the server.
 */
public final class SqlModifierRepository implements ModifierRepository {
    private final ConnectionProvider connections;

    /** @param connections the businesses' connection, since they are written on the same thread */
    public SqlModifierRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<PriceModifier> loadAll() {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "SELECT * FROM price_modifiers ORDER BY created_at");
             ResultSet rows = statement.executeQuery()) {
            List<PriceModifier> modifiers = new ArrayList<>();
            while (rows.next()) {
                long expires = rows.getLong("expires_at");
                Instant expiresAt = rows.wasNull() ? null : Instant.ofEpochMilli(expires);
                modifiers.add(new PriceModifier(rows.getString("id"), rows.getString("type"),
                        ModifierTarget.parse(rows.getString("target")),
                        AppliesTo.fromKey(rows.getString("applies_to")).orElse(AppliesTo.IMPORTS),
                        rows.getBigDecimal("percent"), rows.getString("display_name"), expiresAt,
                        rows.getString("created_by"), Instant.ofEpochMilli(rows.getLong("created_at"))));
            }
            return modifiers;
        } catch (SQLException e) {
            throw new StorageException("Failed to load price modifiers", e);
        }
    }

    @Override
    public void save(PriceModifier modifier) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO price_modifiers (id, type, target, applies_to, percent, display_name, expires_at, created_by, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE type = VALUES(type), target = VALUES(target), "
                        + "applies_to = VALUES(applies_to), percent = VALUES(percent), display_name = VALUES(display_name), "
                        + "expires_at = VALUES(expires_at), created_by = VALUES(created_by), created_at = VALUES(created_at)")) {
            statement.setString(1, modifier.id());
            statement.setString(2, modifier.type());
            statement.setString(3, modifier.target().key());
            statement.setString(4, modifier.appliesTo().key());
            statement.setBigDecimal(5, modifier.percent());
            statement.setString(6, modifier.displayName());
            if (modifier.expiresAt() == null) {
                statement.setNull(7, Types.BIGINT);
            } else {
                statement.setLong(7, modifier.expiresAt().toEpochMilli());
            }
            statement.setString(8, modifier.createdBy());
            statement.setLong(9, modifier.createdAt().toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save price modifier " + modifier.id(), e);
        }
    }

    @Override
    public void delete(String id) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM price_modifiers WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete price modifier " + id, e);
        }
    }
}
