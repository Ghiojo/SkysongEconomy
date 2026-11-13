package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.business.npc.benefit.NpcApplied;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcAppliedRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Saves what the business system changed on each NPC's sheet in the {@code business_npc_applied}
 * table: one row per baked stat ({@code kind} stat) and per raised proficiency ({@code kind}
 * proficiency). Rows go with their {@code business_npcs} row. It blocks on the database, so wrap
 * it in {@link AsyncNpcAppliedRepository} on the server.
 */
public final class SqlNpcAppliedRepository implements NpcAppliedRepository {
    private static final String STAT = "stat";
    private static final String PROFICIENCY = "proficiency";

    private final ConnectionProvider connections;

    public SqlNpcAppliedRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public Map<UUID, NpcApplied> loadAll() {
        Map<UUID, Map<String, NpcApplied.Stat>> stats = new LinkedHashMap<>();
        Map<UUID, Map<String, NpcApplied.Proficiency>> proficiencies = new LinkedHashMap<>();
        try {
            Connection connection = connections.getConnection();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM business_npc_applied");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    UUID npc = UUID.fromString(rows.getString("npc_uuid"));
                    String target = rows.getString("target");
                    if (STAT.equals(rows.getString("kind"))) {
                        stats.computeIfAbsent(npc, id -> new LinkedHashMap<>())
                                .put(target, new NpcApplied.Stat(rows.getString("modifier_id"), rows.getInt("amount")));
                    } else {
                        proficiencies.computeIfAbsent(npc, id -> new LinkedHashMap<>()).put(target,
                                new NpcApplied.Proficiency(rows.getString("tier_before"), rows.getString("tier_applied")));
                    }
                }
            }
        } catch (SQLException | RuntimeException e) {
            throw new StorageException("Failed to load what was changed on business NPCs' sheets", e);
        }
        Map<UUID, NpcApplied> applied = new LinkedHashMap<>();
        stats.keySet().forEach(npc -> applied.put(npc, NpcApplied.NONE));
        proficiencies.keySet().forEach(npc -> applied.put(npc, NpcApplied.NONE));
        applied.replaceAll((npc, none) -> new NpcApplied(stats.getOrDefault(npc, Map.of()),
                proficiencies.getOrDefault(npc, Map.of())));
        return applied;
    }

    @Override
    public void save(UUID npc, NpcApplied applied) {
        try {
            Connection connection = connections.getConnection();
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM business_npc_applied WHERE npc_uuid = ?")) {
                delete.setString(1, npc.toString());
                delete.executeUpdate();
            }
            if (applied.isEmpty()) {
                return;
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO business_npc_applied (npc_uuid, kind, target, modifier_id, amount, tier_before, tier_applied) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                for (Map.Entry<String, NpcApplied.Stat> stat : applied.stats().entrySet()) {
                    insert.setString(1, npc.toString());
                    insert.setString(2, STAT);
                    insert.setString(3, stat.getKey());
                    insert.setString(4, stat.getValue().modifierId());
                    insert.setInt(5, stat.getValue().amount());
                    insert.setString(6, null);
                    insert.setString(7, null);
                    insert.addBatch();
                }
                for (Map.Entry<String, NpcApplied.Proficiency> proficiency : applied.proficiencies().entrySet()) {
                    insert.setString(1, npc.toString());
                    insert.setString(2, PROFICIENCY);
                    insert.setString(3, proficiency.getKey());
                    insert.setString(4, null);
                    insert.setObject(5, null);
                    insert.setString(6, proficiency.getValue().before());
                    insert.setString(7, proficiency.getValue().applied());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to save what was changed on the sheet of the business NPC " + npc, e);
        }
    }
}
