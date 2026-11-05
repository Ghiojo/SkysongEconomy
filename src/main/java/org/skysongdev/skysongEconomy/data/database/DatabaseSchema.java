package org.skysongdev.skysongEconomy.data.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Creates the plugin's tables if they do not exist yet. The old {@code import_slots} and
 * {@code economy_businesses} tables are no longer used or created, but never dropped, so their
 * rows stay for anyone who wants them.
 */
public final class DatabaseSchema {
    private static final List<String> TABLES = List.of(
            "CREATE TABLE IF NOT EXISTS price_ledger ("
                    + "materialId VARCHAR(100) PRIMARY KEY NOT NULL, "
                    + "cost FLOAT DEFAULT 0, "
                    + "expression VARCHAR(255) NULL)",
            "CREATE TABLE IF NOT EXISTS good_types ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "goodType VARCHAR(100), "
                    + "materialId VARCHAR(100))",
            "CREATE TABLE IF NOT EXISTS businesses ("
                    + "id VARCHAR(64) PRIMARY KEY NOT NULL, "
                    + "display_name VARCHAR(255) NOT NULL, "
                    + "type_id VARCHAR(64) NOT NULL, "
                    + "tier TINYINT NOT NULL, "
                    + "owner_uuid CHAR(36) NOT NULL, "
                    + "owner_profile VARCHAR(64) NOT NULL, "
                    + "world VARCHAR(255) NULL, "
                    + "x INT NULL, y INT NULL, z INT NULL, "
                    + "shift_message VARCHAR(255) NULL) ENGINE=InnoDB",
            "CREATE TABLE IF NOT EXISTS business_workers ("
                    + "business_id VARCHAR(64) NOT NULL, "
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "profile VARCHAR(64) NOT NULL, "
                    + "hired_at BIGINT NOT NULL, "
                    + "PRIMARY KEY (business_id, player_uuid, profile), "
                    + "FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE) ENGINE=InnoDB",
            // The same skill can be picked more than once, so picks get their own id.
            "CREATE TABLE IF NOT EXISTS business_skill_picks ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "business_id VARCHAR(64) NOT NULL, "
                    + "skill_id VARCHAR(64) NOT NULL, "
                    + "picked_at BIGINT NOT NULL, "
                    + "INDEX (business_id), "
                    + "FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE) ENGINE=InnoDB",
            // position keeps the owner's groups in the order they were made.
            "CREATE TABLE IF NOT EXISTS business_access_groups ("
                    + "business_id VARCHAR(64) NOT NULL, "
                    + "group_id VARCHAR(32) NOT NULL, "
                    + "position INT NOT NULL, "
                    + "permissions VARCHAR(255) NOT NULL, "
                    + "training_price DECIMAL(19, 4) NOT NULL, "
                    + "PRIMARY KEY (business_id, group_id), "
                    + "FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE) ENGINE=InnoDB",
            "CREATE TABLE IF NOT EXISTS business_access_members ("
                    + "business_id VARCHAR(64) NOT NULL, "
                    + "group_id VARCHAR(32) NOT NULL, "
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "profile VARCHAR(64) NOT NULL, "
                    + "PRIMARY KEY (business_id, group_id, player_uuid, profile), "
                    + "FOREIGN KEY (business_id, group_id) REFERENCES business_access_groups(business_id, group_id) "
                    + "ON DELETE CASCADE) ENGINE=InnoDB",
            "CREATE TABLE IF NOT EXISTS business_vaults ("
                    + "business_id VARCHAR(64) PRIMARY KEY NOT NULL, "
                    + "balance DECIMAL(19, 4) NOT NULL, "
                    + "FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE) ENGINE=InnoDB",
            "CREATE TABLE IF NOT EXISTS gathering_tokens ("
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "profile VARCHAR(64) NOT NULL, "
                    + "balance BIGINT NOT NULL, "
                    + "PRIMARY KEY (player_uuid, profile)) ENGINE=InnoDB",
            // target is written like ledger:smugglers; expires_at is NULL for never.
            "CREATE TABLE IF NOT EXISTS price_modifiers ("
                    + "id VARCHAR(16) PRIMARY KEY NOT NULL, "
                    + "type VARCHAR(32) NOT NULL, "
                    + "target VARCHAR(128) NOT NULL, "
                    + "applies_to VARCHAR(16) NOT NULL, "
                    + "percent DECIMAL(10, 4) NOT NULL, "
                    + "display_name VARCHAR(255) NOT NULL, "
                    + "expires_at BIGINT NULL, "
                    + "created_by VARCHAR(64) NOT NULL, "
                    + "created_at BIGINT NOT NULL, "
                    + "INDEX (target)) ENGINE=InnoDB",
            // A shift is kept from its start until its pay is retrieved; finished_at is NULL while it runs.
            "CREATE TABLE IF NOT EXISTS business_shifts ("
                    + "id CHAR(36) PRIMARY KEY NOT NULL, "
                    + "business_id VARCHAR(64) NOT NULL, "
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "profile VARCHAR(64) NOT NULL, "
                    + "started_at BIGINT NOT NULL, "
                    + "required_seconds BIGINT NOT NULL, "
                    + "worked_seconds BIGINT NOT NULL, "
                    + "money DECIMAL(19, 4) NOT NULL, "
                    + "tokens BIGINT NOT NULL, "
                    + "vault_cut DECIMAL(19, 4) NOT NULL, "
                    + "finished_at BIGINT NULL, "
                    + "INDEX (business_id), "
                    + "FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE) ENGINE=InnoDB",
            // Only running sessions are kept: a finished one gives its rewards and is deleted.
            "CREATE TABLE IF NOT EXISTS business_training ("
                    + "id CHAR(36) PRIMARY KEY NOT NULL, "
                    + "business_id VARCHAR(64) NOT NULL, "
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "profile VARCHAR(64) NOT NULL, "
                    + "started_at BIGINT NOT NULL, "
                    + "required_seconds BIGINT NOT NULL, "
                    + "worked_seconds BIGINT NOT NULL, "
                    + "price DECIMAL(19, 4) NOT NULL, "
                    + "INDEX (business_id), "
                    + "FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE) ENGINE=InnoDB");

    private DatabaseSchema() {
    }

    /** Columns added after their table first shipped: table, column, definition. */
    private static final List<String[]> ADDED_COLUMNS = List.<String[]>of(
            new String[] {"price_ledger", "expression", "VARCHAR(255) NULL"});

    public static void createTables(ConnectionProvider connections) throws SQLException {
        Connection connection = connections.getConnection();
        try (Statement statement = connection.createStatement()) {
            for (String table : TABLES) {
                statement.execute(table);
            }
        }
        for (String[] column : ADDED_COLUMNS) {
            addColumnIfMissing(connection, column[0], column[1], column[2]);
        }
    }

    /** MySQL has no ADD COLUMN IF NOT EXISTS, so ask information_schema first. */
    private static void addColumnIfMissing(Connection connection, String table, String column, String definition)
            throws SQLException {
        try (PreparedStatement check = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            check.setString(1, table);
            check.setString(2, column);
            try (ResultSet result = check.executeQuery()) {
                if (result.next() && result.getInt(1) > 0) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
