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
                    + "world VARCHAR(255))");

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
