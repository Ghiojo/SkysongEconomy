package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.AccessRepository;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Saves access groups in the {@code business_access_groups} and {@code business_access_members}
 * tables. Permissions are stored as their config names separated by commas, e.g.
 * {@code train,sell-exports}. A save rewrites all of a business's groups in one transaction.
 * <p>
 * It blocks on the database, so wrap it in {@link AsyncAccessRepository} on the server.
 */
public final class SqlAccessRepository implements AccessRepository {
    private final ConnectionProvider connections;

    /** @param connections the businesses' connection, since they are written on the same thread */
    public SqlAccessRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public Map<String, List<AccessGroup>> loadAll() {
        try {
            Connection connection = connections.getConnection();
            Map<String, Set<CharacterRef>> members = loadMembers(connection);
            Map<String, List<AccessGroup>> groups = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM business_access_groups ORDER BY business_id, position");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String businessId = rows.getString("business_id");
                    String groupId = rows.getString("group_id");
                    groups.computeIfAbsent(businessId, id -> new ArrayList<>()).add(new AccessGroup(groupId,
                            readPermissions(rows.getString("permissions")),
                            rows.getBigDecimal("training_price").stripTrailingZeros(),
                            members.getOrDefault(key(businessId, groupId), Set.of())));
                }
            }
            return groups;
        } catch (SQLException e) {
            throw new StorageException("Failed to load access groups", e);
        }
    }

    @Override
    public void save(String businessId, List<AccessGroup> groups) {
        SqlTransaction.run(connections, "Failed to save access groups", connection -> {
            // Deleting the groups also deletes their members (ON DELETE CASCADE).
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM business_access_groups WHERE business_id = ?")) {
                statement.setString(1, businessId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO business_access_groups (business_id, group_id, position, permissions, training_price) "
                            + "VALUES (?, ?, ?, ?, ?)")) {
                for (int i = 0; i < groups.size(); i++) {
                    AccessGroup group = groups.get(i);
                    statement.setString(1, businessId);
                    statement.setString(2, group.id());
                    statement.setInt(3, i);
                    statement.setString(4, writePermissions(group.permissions()));
                    statement.setBigDecimal(5, group.trainingPrice());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO business_access_members (business_id, group_id, player_uuid, profile) VALUES (?, ?, ?, ?)")) {
                for (AccessGroup group : groups) {
                    for (CharacterRef member : group.members()) {
                        statement.setString(1, businessId);
                        statement.setString(2, group.id());
                        statement.setString(3, member.player().toString());
                        statement.setString(4, member.profile());
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            }
        });
    }

    private static Map<String, Set<CharacterRef>> loadMembers(Connection connection) throws SQLException {
        Map<String, Set<CharacterRef>> members = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM business_access_members");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                members.computeIfAbsent(key(rows.getString("business_id"), rows.getString("group_id")), id -> new HashSet<>())
                        .add(new CharacterRef(UUID.fromString(rows.getString("player_uuid")), rows.getString("profile")));
            }
        }
        return members;
    }

    private static String key(String businessId, String groupId) {
        return businessId + '/' + groupId;
    }

    /** Unknown names, e.g. from a newer version of the plugin, are skipped. */
    private static Set<Permission> readPermissions(String stored) {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        for (String key : stored.split(",")) {
            Permission.fromKey(key.strip()).ifPresent(permissions::add);
        }
        return permissions;
    }

    private static String writePermissions(Set<Permission> permissions) {
        return permissions.stream().sorted().map(Permission::key).collect(Collectors.joining(","));
    }
}
