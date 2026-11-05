package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.tokens.TokenRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Saves Gathering Token balances in the {@code gathering_tokens} table, one row per character.
 * It blocks on the database, so wrap it in {@link AsyncTokenRepository} on the server.
 */
public final class SqlTokenRepository implements TokenRepository {
    private final ConnectionProvider connections;

    /** @param connections the businesses' connection, since they are written on the same thread */
    public SqlTokenRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public Map<CharacterRef, Long> loadAll() {
        try {
            Connection connection = connections.getConnection();
            Map<CharacterRef, Long> balances = new HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM gathering_tokens");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    balances.put(new CharacterRef(UUID.fromString(rows.getString("player_uuid")), rows.getString("profile")),
                            rows.getLong("balance"));
                }
            }
            return balances;
        } catch (SQLException e) {
            throw new StorageException("Failed to load Gathering Tokens", e);
        }
    }

    @Override
    public void save(CharacterRef character, long balance) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO gathering_tokens (player_uuid, profile, balance) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE balance = VALUES(balance)")) {
            statement.setString(1, character.player().toString());
            statement.setString(2, character.profile());
            statement.setLong(3, balance);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the Gathering Tokens of " + character, e);
        }
    }
}
