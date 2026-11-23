package org.skysongdev.skysongEconomy.data.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.skysongdev.skysongEconomy.api.piers.LossModifier;
import org.skysongdev.skysongEconomy.api.piers.PierDetails;
import org.skysongdev.skysongEconomy.trade.pier.PierRepository;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Saves piers in the {@code piers} table and their loss modifiers in {@code pier_loss_modifiers}.
 * {@code description} and {@code data} are JSON. It blocks on the database, so wrap it in
 * {@link AsyncPierRepository} on the server.
 */
public final class SqlPierRepository implements PierRepository {
    private static final Gson GSON = new Gson();
    private static final Type LINES = new TypeToken<List<String>>() { }.getType();
    private static final Type DATA = new TypeToken<Map<String, String>>() { }.getType();

    private final ConnectionProvider connections;

    public SqlPierRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<Row> loadPiers() {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "SELECT id, source, name, icon, description, open, data, loss_chance FROM piers ORDER BY created_at, id");
             ResultSet rows = statement.executeQuery()) {
            List<Row> piers = new ArrayList<>();
            while (rows.next()) {
                String name = rows.getString("name");
                PierDetails details = name == null ? null : new PierDetails(name, rows.getString("icon"),
                        GSON.<List<String>>fromJson(rows.getString("description"), LINES));
                Map<String, String> data = GSON.fromJson(rows.getString("data"), DATA);
                piers.add(new Row(rows.getString("id"), rows.getString("source"), details, rows.getBoolean("open"),
                        data == null ? Map.of() : data, rows.getBigDecimal("loss_chance")));
            }
            return piers;
        } catch (SQLException e) {
            throw new StorageException("Failed to load piers", e);
        }
    }

    @Override
    public List<ModifierRow> loadModifiers() {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "SELECT pier_id, modifier_id, amount, percent, tag FROM pier_loss_modifiers ORDER BY set_at, modifier_id");
             ResultSet rows = statement.executeQuery()) {
            List<ModifierRow> modifiers = new ArrayList<>();
            while (rows.next()) {
                modifiers.add(new ModifierRow(rows.getString("pier_id"), new LossModifier(rows.getString("modifier_id"),
                        rows.getBigDecimal("amount"), rows.getBoolean("percent"), Optional.ofNullable(rows.getString("tag")))));
            }
            return modifiers;
        } catch (SQLException e) {
            throw new StorageException("Failed to load pier loss modifiers", e);
        }
    }

    @Override
    public void savePier(Row row) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO piers (id, source, name, icon, description, open, data, loss_chance, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE source = VALUES(source), "
                        + "name = VALUES(name), icon = VALUES(icon), description = VALUES(description), "
                        + "open = VALUES(open), data = VALUES(data), loss_chance = VALUES(loss_chance)")) {
            statement.setString(1, row.id());
            statement.setString(2, row.source());
            statement.setString(3, row.details() == null ? null : row.details().displayName());
            statement.setString(4, row.details() == null ? null : row.details().icon());
            statement.setString(5, row.details() == null ? null : GSON.toJson(row.details().description()));
            statement.setBoolean(6, row.open());
            statement.setString(7, GSON.toJson(row.data()));
            statement.setBigDecimal(8, row.lossChance());
            statement.setLong(9, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the pier " + row.id(), e);
        }
    }

    @Override
    public void deletePier(String id) {
        SqlTransaction.run(connections, "Failed to delete the pier " + id, connection -> {
            delete(connection, "DELETE FROM pier_loss_modifiers WHERE pier_id = ?", id);
            delete(connection, "DELETE FROM piers WHERE id = ?", id);
        });
    }

    @Override
    public void saveModifier(ModifierRow row) {
        LossModifier modifier = row.modifier();
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO pier_loss_modifiers (pier_id, modifier_id, amount, percent, tag, set_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE amount = VALUES(amount), "
                        + "percent = VALUES(percent), tag = VALUES(tag)")) {
            statement.setString(1, row.pierId());
            statement.setString(2, modifier.id());
            statement.setBigDecimal(3, modifier.amount());
            statement.setBoolean(4, modifier.percent());
            statement.setString(5, modifier.tag().orElse(null));
            statement.setLong(6, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the loss modifier " + modifier.id() + " of the pier " + row.pierId(), e);
        }
    }

    @Override
    public void deleteModifier(String pierId, String modifierId) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM pier_loss_modifiers WHERE pier_id = ? AND modifier_id = ?")) {
            statement.setString(1, pierId);
            statement.setString(2, modifierId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the loss modifier " + modifierId + " of the pier " + pierId, e);
        }
    }

    private static void delete(Connection connection, String sql, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.executeUpdate();
        }
    }
}
