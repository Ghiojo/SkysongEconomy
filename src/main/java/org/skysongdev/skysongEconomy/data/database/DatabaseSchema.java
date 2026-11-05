package org.skysongdev.skysongEconomy.data.database;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Creates the plugin's tables if they do not exist yet. */
public final class DatabaseSchema {
    private static final List<String> TABLES = List.of(
            "CREATE TABLE IF NOT EXISTS price_ledger ("
                    + "materialId VARCHAR(100) PRIMARY KEY NOT NULL, "
                    + "cost FLOAT DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS good_types ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "goodType VARCHAR(100), "
                    + "materialId VARCHAR(100))",
            "CREATE TABLE IF NOT EXISTS import_slots ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "business_id VARCHAR(100), "
                    + "slot_id INT, "
                    + "import_id VARCHAR(100), "
                    + "arrival_date DATETIME)",
            "CREATE TABLE IF NOT EXISTS economy_businesses ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "business_id VARCHAR(100), "
                    + "owner_uuid VARCHAR(100), "
                    + "business_name VARCHAR(100), "
                    + "member_uuids VARCHAR(5000), "
                    + "catalog_ids VARCHAR(5000), "
                    + "x INT, y INT, z INT, "
                    + "world VARCHAR(255))",
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
                    + "FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE) ENGINE=InnoDB");

    private DatabaseSchema() {
    }

    public static void createTables(ConnectionProvider connections) throws SQLException {
        try (Statement statement = connections.getConnection().createStatement()) {
            for (String table : TABLES) {
                statement.execute(table);
            }
        }
    }
}
