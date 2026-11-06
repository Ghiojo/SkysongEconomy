package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.nodes.NodeKind;
import org.skysongdev.skysongEconomy.nodes.NodeOwner;
import org.skysongdev.skysongEconomy.nodes.NodeRepository;
import org.skysongdev.skysongEconomy.nodes.NodeSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves nodes in the {@code nodes} table. The owner is two columns: {@code owner_kind}
 * ({@code public}, {@code unowned} or {@code business}) and {@code owner_business}, so a deleted
 * business's nodes are told apart from public ones.
 * <p>
 * It blocks on the database, so wrap it in {@link AsyncNodeRepository} on the server. A row
 * whose kind or owner is unknown is skipped, so one bad row doesn't stop the rest loading.
 */
public final class SqlNodeRepository implements NodeRepository {
    private static final String PUBLIC = "public";
    private static final String UNOWNED = "unowned";
    private static final String BUSINESS = "business";

    private final ConnectionProvider connections;

    public SqlNodeRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<NodeSnapshot> loadAll() {
        List<NodeSnapshot> nodes = new ArrayList<>();
        try (PreparedStatement statement = connections.getConnection().prepareStatement("SELECT * FROM nodes");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                NodeKind kind = NodeKind.fromKey(rows.getString("kind")).orElse(null);
                NodeOwner owner = readOwner(rows);
                if (kind == null || owner == null) {
                    continue;
                }
                nodes.add(new NodeSnapshot(rows.getString("id"), kind, rows.getString("source"), rows.getInt("tier"),
                        owner, rows.getString("display_name"), readPosition(rows)));
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to load nodes", e);
        }
        return nodes;
    }

    @Override
    public void save(NodeSnapshot node) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO nodes (id, kind, source, tier, owner_kind, owner_business, display_name, world, x, y, z) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                        + "kind = VALUES(kind), source = VALUES(source), tier = VALUES(tier), "
                        + "owner_kind = VALUES(owner_kind), owner_business = VALUES(owner_business), "
                        + "display_name = VALUES(display_name), world = VALUES(world), "
                        + "x = VALUES(x), y = VALUES(y), z = VALUES(z)")) {
            statement.setString(1, node.id());
            statement.setString(2, node.kind().key());
            statement.setString(3, node.source());
            statement.setInt(4, node.tier());
            writeOwner(statement, node.owner());
            statement.setString(7, node.displayName());
            writePosition(statement, node.position());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the node " + node.id(), e);
        }
    }

    @Override
    public void delete(String id) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement("DELETE FROM nodes WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the node " + id, e);
        }
    }

    private static NodeOwner readOwner(ResultSet rows) throws SQLException {
        String business = rows.getString("owner_business");
        return switch (rows.getString("owner_kind")) {
            case PUBLIC -> NodeOwner.PUBLIC;
            case UNOWNED -> NodeOwner.UNOWNED;
            case BUSINESS -> business == null ? null : new NodeOwner.Business(business);
            default -> null;
        };
    }

    private static void writeOwner(PreparedStatement statement, NodeOwner owner) throws SQLException {
        switch (owner) {
            case NodeOwner.Public ignored -> {
                statement.setString(5, PUBLIC);
                statement.setNull(6, Types.VARCHAR);
            }
            case NodeOwner.Unowned ignored -> {
                statement.setString(5, UNOWNED);
                statement.setNull(6, Types.VARCHAR);
            }
            case NodeOwner.Business business -> {
                statement.setString(5, BUSINESS);
                statement.setString(6, business.businessId());
            }
        }
    }

    private static BlockPosition readPosition(ResultSet rows) throws SQLException {
        String world = rows.getString("world");
        return world == null ? null : new BlockPosition(world, rows.getInt("x"), rows.getInt("y"), rows.getInt("z"));
    }

    private static void writePosition(PreparedStatement statement, BlockPosition position) throws SQLException {
        if (position == null) {
            statement.setNull(8, Types.VARCHAR);
            statement.setNull(9, Types.INTEGER);
            statement.setNull(10, Types.INTEGER);
            statement.setNull(11, Types.INTEGER);
            return;
        }
        statement.setString(8, position.world());
        statement.setInt(9, position.x());
        statement.setInt(10, position.y());
        statement.setInt(11, position.z());
    }
}
