package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.BusinessRepository;
import org.skysongdev.skysongEconomy.business.BusinessSnapshot;
import org.skysongdev.skysongEconomy.business.Hire;
import org.skysongdev.skysongEconomy.business.SkillPick;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saves businesses in the {@code businesses}, {@code business_workers} and
 * {@code business_skill_picks} tables. A save replaces the business row and all its worker and
 * pick rows in one transaction.
 * <p>
 * It blocks on the database, so wrap it in {@link AsyncBusinessRepository} on the server. It
 * uses transactions, so only share its {@link ConnectionProvider} with repositories that write
 * on the same {@link BackgroundWriter} (such as {@link SqlAccessRepository}): otherwise their
 * statements could land inside its transactions. Deleting a business deletes its access groups,
 * vault, shifts and training sessions.
 */
public final class SqlBusinessRepository implements BusinessRepository {
    private final ConnectionProvider connections;

    public SqlBusinessRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<BusinessSnapshot> loadAll() {
        try {
            Connection connection = connections.getConnection();
            Map<String, List<Hire>> workers = loadWorkers(connection);
            Map<String, List<SkillPick>> picks = loadPicks(connection);
            List<BusinessSnapshot> businesses = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM businesses");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String id = rows.getString("id");
                    businesses.add(new BusinessSnapshot(id, rows.getString("display_name"), rows.getString("type_id"),
                            rows.getInt("tier"),
                            new CharacterRef(UUID.fromString(rows.getString("owner_uuid")), rows.getString("owner_profile")),
                            readPosition(rows), rows.getString("shift_message"),
                            workers.getOrDefault(id, List.of()), picks.getOrDefault(id, List.of())));
                }
            }
            return businesses;
        } catch (SQLException e) {
            throw new StorageException("Failed to load businesses", e);
        }
    }

    @Override
    public void save(BusinessSnapshot business) {
        inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO businesses (id, display_name, type_id, tier, owner_uuid, owner_profile, world, x, y, z, shift_message) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                            + "display_name = VALUES(display_name), type_id = VALUES(type_id), tier = VALUES(tier), "
                            + "owner_uuid = VALUES(owner_uuid), owner_profile = VALUES(owner_profile), world = VALUES(world), "
                            + "x = VALUES(x), y = VALUES(y), z = VALUES(z), shift_message = VALUES(shift_message)")) {
                statement.setString(1, business.id());
                statement.setString(2, business.displayName());
                statement.setString(3, business.typeId());
                statement.setInt(4, business.tier());
                statement.setString(5, business.owner().player().toString());
                statement.setString(6, business.owner().profile());
                writePosition(statement, business.position());
                statement.setString(11, business.shiftMessage());
                statement.executeUpdate();
            }
            // The worker rows are rewritten as a whole: simpler than diffing, and a business has few.
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM business_workers WHERE business_id = ?")) {
                statement.setString(1, business.id());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO business_workers (business_id, player_uuid, profile, hired_at) VALUES (?, ?, ?, ?)")) {
                for (Hire hire : business.workers()) {
                    statement.setString(1, business.id());
                    statement.setString(2, hire.character().player().toString());
                    statement.setString(3, hire.character().profile());
                    statement.setLong(4, hire.hiredAt().toEpochMilli());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM business_skill_picks WHERE business_id = ?")) {
                statement.setString(1, business.id());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO business_skill_picks (business_id, skill_id, picked_at) VALUES (?, ?, ?)")) {
                for (SkillPick pick : business.picks()) {
                    statement.setString(1, business.id());
                    statement.setString(2, pick.skillId());
                    statement.setLong(3, pick.pickedAt().toEpochMilli());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    @Override
    public void delete(String id) {
        inTransaction(connection -> {
            for (String child : List.of(
                    "business_workers", "business_skill_picks", "business_access_members", "business_access_groups",
                    "business_vaults", "business_shifts", "business_training")) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + child + " WHERE business_id = ?")) {
                    statement.setString(1, id);
                    statement.executeUpdate();
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM businesses WHERE id = ?")) {
                statement.setString(1, id);
                statement.executeUpdate();
            }
        });
    }

    private static Map<String, List<Hire>> loadWorkers(Connection connection) throws SQLException {
        Map<String, List<Hire>> workers = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM business_workers ORDER BY hired_at");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                CharacterRef character = new CharacterRef(UUID.fromString(rows.getString("player_uuid")), rows.getString("profile"));
                workers.computeIfAbsent(rows.getString("business_id"), id -> new ArrayList<>())
                        .add(new Hire(character, Instant.ofEpochMilli(rows.getLong("hired_at"))));
            }
        }
        return workers;
    }

    private static Map<String, List<SkillPick>> loadPicks(Connection connection) throws SQLException {
        Map<String, List<SkillPick>> picks = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM business_skill_picks ORDER BY picked_at, id");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                picks.computeIfAbsent(rows.getString("business_id"), id -> new ArrayList<>())
                        .add(new SkillPick(rows.getString("skill_id"), Instant.ofEpochMilli(rows.getLong("picked_at"))));
            }
        }
        return picks;
    }

    private static BlockPosition readPosition(ResultSet rows) throws SQLException {
        String world = rows.getString("world");
        if (world == null) {
            return null;
        }
        return new BlockPosition(world, rows.getInt("x"), rows.getInt("y"), rows.getInt("z"));
    }

    private static void writePosition(PreparedStatement statement, BlockPosition position) throws SQLException {
        if (position == null) {
            statement.setNull(7, Types.VARCHAR);
            statement.setNull(8, Types.INTEGER);
            statement.setNull(9, Types.INTEGER);
            statement.setNull(10, Types.INTEGER);
            return;
        }
        statement.setString(7, position.world());
        statement.setInt(8, position.x());
        statement.setInt(9, position.y());
        statement.setInt(10, position.z());
    }

    private void inTransaction(SqlTransaction.Work work) {
        SqlTransaction.run(connections, "Failed to save businesses", work);
    }
}
