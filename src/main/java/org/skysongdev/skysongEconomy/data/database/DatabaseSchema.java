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
                    + "owner_uuid CHAR(36) NULL, "
                    + "owner_profile VARCHAR(64) NULL, "
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
            "CREATE TABLE IF NOT EXISTS business_ledgers ("
                    + "business_id VARCHAR(64) NOT NULL, "
                    + "ledger_id VARCHAR(100) NOT NULL, "
                    + "selected_at BIGINT NOT NULL, "
                    + "PRIMARY KEY (business_id, ledger_id)) ENGINE=InnoDB",
            // items is written like steel_sword:2;steel_spear:3, rolled when the import was ordered.
            "CREATE TABLE IF NOT EXISTS business_deliveries ("
                    + "id CHAR(36) PRIMARY KEY NOT NULL, "
                    + "business_id VARCHAR(64) NOT NULL, "
                    + "ledger_id VARCHAR(100) NOT NULL, "
                    + "import_id VARCHAR(100) NOT NULL, "
                    + "delivery_time_id VARCHAR(64) NOT NULL, "
                    + "items TEXT NOT NULL, "
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "profile VARCHAR(64) NOT NULL, "
                    + "price DECIMAL(19, 4) NOT NULL, "
                    + "ordered_at BIGINT NOT NULL, "
                    + "arrives_at BIGINT NOT NULL, "
                    + "INDEX (business_id)) ENGINE=InnoDB",
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
                    + "FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE) ENGINE=InnoDB",
            // Gathering nodes. The owning business has no foreign key on purpose: when a business is
            // deleted its nodes become 'unowned' (not public), which the plugin does itself.
            "CREATE TABLE IF NOT EXISTS nodes ("
                    + "id VARCHAR(64) PRIMARY KEY NOT NULL, "
                    + "kind VARCHAR(16) NOT NULL, "
                    + "source VARCHAR(64) NOT NULL, "
                    + "tier TINYINT NOT NULL, "
                    + "owner_kind VARCHAR(16) NOT NULL, "
                    + "owner_business VARCHAR(64) NULL, "
                    + "display_name VARCHAR(255) NULL, "
                    + "world VARCHAR(255) NULL, "
                    + "x INT NULL, y INT NULL, z INT NULL) ENGINE=InnoDB",
            // What is planted in each farm node: the exact primer items, so the same ones come back.
            "CREATE TABLE IF NOT EXISTS node_plantings ("
                    + "node_id VARCHAR(64) PRIMARY KEY NOT NULL, "
                    + "resource_id VARCHAR(64) NOT NULL, "
                    + "farm_type VARCHAR(64) NOT NULL, "
                    + "business_id VARCHAR(64) NOT NULL, "
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "profile VARCHAR(64) NOT NULL, "
                    + "items MEDIUMBLOB NOT NULL, "
                    + "FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE) ENGINE=InnoDB",
            // Primers waiting to go back to a planter who was offline.
            "CREATE TABLE IF NOT EXISTS node_primer_returns ("
                    + "id CHAR(36) PRIMARY KEY NOT NULL, "
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "items MEDIUMBLOB NOT NULL, "
                    + "INDEX (player_uuid)) ENGINE=InnoDB",
            // The SkysongNPCs each business has. source is the slot it fills, such as skill:martial.
            "CREATE TABLE IF NOT EXISTS business_npcs ("
                    + "npc_uuid CHAR(36) PRIMARY KEY NOT NULL, "
                    + "business_id VARCHAR(64) NOT NULL, "
                    + "source VARCHAR(128) NOT NULL, "
                    + "created_at BIGINT NOT NULL, "
                    + "INDEX (business_id), "
                    + "FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE) ENGINE=InnoDB",
            // What the plugin changed on each business NPC's SkysongStats sheet, to undo exactly that:
            // kind 'stat' = a baked bonus (modifier_id, amount), kind 'proficiency' = a raised tier.
            "CREATE TABLE IF NOT EXISTS business_npc_applied ("
                    + "npc_uuid CHAR(36) NOT NULL, "
                    + "kind VARCHAR(16) NOT NULL, "
                    + "target VARCHAR(64) NOT NULL, "
                    + "modifier_id VARCHAR(128) NULL, "
                    + "amount INT NULL, "
                    + "tier_before VARCHAR(64) NULL, "
                    + "tier_applied VARCHAR(64) NULL, "
                    + "PRIMARY KEY (npc_uuid, kind, target), "
                    + "FOREIGN KEY (npc_uuid) REFERENCES business_npcs(npc_uuid) ON DELETE CASCADE) ENGINE=InnoDB",
            // The modules each business picked. pick_order counts up within a business: higher is newer.
            "CREATE TABLE IF NOT EXISTS business_modules ("
                    + "business_id VARCHAR(64) NOT NULL, "
                    + "pick_order INT NOT NULL, "
                    + "skill VARCHAR(64) NOT NULL, "
                    + "module_id VARCHAR(64) NOT NULL, "
                    + "picked_at BIGINT NOT NULL, "
                    + "PRIMARY KEY (business_id, pick_order), "
                    + "FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE) ENGINE=InnoDB",
            // The NPCs an owner chose for a module benefit that goes to a number of NPCs. effect is the
            // effect's place in the module file, from 0.
            "CREATE TABLE IF NOT EXISTS business_module_targets ("
                    + "business_id VARCHAR(64) NOT NULL, "
                    + "pick_order INT NOT NULL, "
                    + "effect INT NOT NULL, "
                    + "npc_uuid CHAR(36) NOT NULL, "
                    + "chosen_at BIGINT NOT NULL, "
                    + "PRIMARY KEY (business_id, pick_order, effect, npc_uuid), "
                    + "INDEX (npc_uuid), "
                    + "FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE) ENGINE=InnoDB",
            // NPC upkeep: one row per NPC that has an upkeep, or that staff left without one on
            // purpose (profile NULL). state is MAINTAINED or UNMAINTAINED; times are epoch millis.
            "CREATE TABLE IF NOT EXISTS npc_upkeep ("
                    + "npc_uuid CHAR(36) PRIMARY KEY NOT NULL, "
                    + "profile VARCHAR(64) NULL, "
                    + "set_by_hand BOOLEAN NOT NULL, "
                    + "state VARCHAR(16) NOT NULL, "
                    + "next_due BIGINT NOT NULL, "
                    + "changed_at BIGINT NOT NULL) ENGINE=InnoDB",
            // The items stocked in an NPC for its upkeep, in Paper's own item format.
            "CREATE TABLE IF NOT EXISTS npc_upkeep_stock ("
                    + "npc_uuid CHAR(36) PRIMARY KEY NOT NULL, "
                    + "items MEDIUMBLOB NOT NULL) ENGINE=InnoDB",
            // SkysongClaims Claims' asking prices: a price expression per Claim id, before modifiers.
            "CREATE TABLE IF NOT EXISTS claim_prices ("
                    + "claim_id VARCHAR(64) PRIMARY KEY NOT NULL, "
                    + "expression VARCHAR(255) NOT NULL, "
                    + "set_by VARCHAR(64) NOT NULL, "
                    + "set_at BIGINT NOT NULL) ENGINE=InnoDB",
            // Every Claim bought, as ClaimPurchaseEvent reported it; modifiers is a JSON list of each modifier's
            // share, seller the JSON of who sold it and what each received (NULL for a server sale).
            "CREATE TABLE IF NOT EXISTS claim_sales ("
                    + "id BIGINT PRIMARY KEY NOT NULL, "
                    + "claim_id VARCHAR(64) NOT NULL, "
                    + "claim_type VARCHAR(64) NOT NULL, "
                    + "buyer_uuid CHAR(36) NOT NULL, "
                    + "buyer_profile VARCHAR(64) NOT NULL, "
                    + "asking_price DECIMAL(19, 4) NOT NULL, "
                    + "modifier_total DECIMAL(19, 4) NOT NULL, "
                    + "paid DECIMAL(19, 4) NOT NULL, "
                    + "modifiers TEXT NOT NULL, "
                    + "seller TEXT NULL, "
                    + "seller_paid_later BOOLEAN NOT NULL DEFAULT FALSE, "
                    + "sold_at BIGINT NOT NULL, "
                    + "INDEX claim_sales_claim (claim_id)) ENGINE=InnoDB",
            // Claims on sale for their owners rather than the server, and the owner who put each on sale.
            "CREATE TABLE IF NOT EXISTS claim_listings ("
                    + "claim_id VARCHAR(64) PRIMARY KEY NOT NULL, "
                    + "listed_by_uuid CHAR(36) NOT NULL, "
                    + "listed_by_profile VARCHAR(64) NOT NULL, "
                    + "listed_at BIGINT NOT NULL) ENGINE=InnoDB",
            // Sellers' shares of Claim sales that couldn't be paid at once, paid when they join.
            "CREATE TABLE IF NOT EXISTS claim_payouts ("
                    + "sale_id BIGINT NOT NULL, "
                    + "account_uuid CHAR(36) NOT NULL, "
                    + "claim_id VARCHAR(64) NOT NULL, "
                    + "amount DECIMAL(19, 4) NOT NULL, "
                    + "sold_at BIGINT NOT NULL, "
                    + "PRIMARY KEY (sale_id, account_uuid)) ENGINE=InnoDB",
            // Piers plugins created (source = the plugin), and changes made to piers from piers.yml
            // (source = config, name NULL so the file's name, icon and description stay). description and
            // data are JSON; loss_chance NULL keeps the file's.
            "CREATE TABLE IF NOT EXISTS piers ("
                    + "id VARCHAR(64) PRIMARY KEY NOT NULL, "
                    + "source VARCHAR(64) NOT NULL, "
                    + "name VARCHAR(255) NULL, "
                    + "icon VARCHAR(64) NULL, "
                    + "description TEXT NULL, "
                    + "open BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "data TEXT NOT NULL, "
                    + "loss_chance DECIMAL(7, 4) NULL, "
                    + "created_at BIGINT NOT NULL) ENGINE=InnoDB",
            // Loss modifiers set through the API or /se pier modifier; tag NULL counts for every shipment.
            "CREATE TABLE IF NOT EXISTS pier_loss_modifiers ("
                    + "pier_id VARCHAR(64) NOT NULL, "
                    + "modifier_id VARCHAR(64) NOT NULL, "
                    + "amount DECIMAL(19, 4) NOT NULL, "
                    + "percent BOOLEAN NOT NULL, "
                    + "tag VARCHAR(64) NULL, "
                    + "set_at BIGINT NOT NULL, "
                    + "PRIMARY KEY (pier_id, modifier_id)) ENGINE=InnoDB",
            // The economy journal: one row per movement of money or goods, never updated. trade, money, items and
            // details are JSON; tags is ",tag,tag," so one tag is found with LIKE. ic_* are NULL without SkysongCalendar,
            // and ic_week is NULL on festival days.
            "CREATE TABLE IF NOT EXISTS economy_journal ("
                    + "sequence BIGINT PRIMARY KEY NOT NULL, "
                    + "record_id CHAR(36) NOT NULL, "
                    + "kind VARCHAR(32) NOT NULL, "
                    + "subject_id VARCHAR(100) NOT NULL, "
                    + "recorded_at BIGINT NOT NULL, "
                    + "ic_year INT NULL, "
                    + "ic_period VARCHAR(64) NULL, "
                    + "ic_day INT NULL, "
                    + "ic_day_of_year INT NULL, "
                    + "ic_week INT NULL, "
                    + "ic_festival BOOLEAN NULL, "
                    + "actor_uuid CHAR(36) NULL, "
                    + "actor_profile VARCHAR(64) NULL, "
                    + "business_id VARCHAR(64) NULL, "
                    + "business_name VARCHAR(255) NULL, "
                    + "pier_id VARCHAR(64) NULL, "
                    + "node_id VARCHAR(64) NULL, "
                    + "claim_id VARCHAR(64) NULL, "
                    + "tags VARCHAR(512) NULL, "
                    + "trade TEXT NULL, "
                    + "money TEXT NOT NULL, "
                    + "items TEXT NOT NULL, "
                    + "details TEXT NOT NULL, "
                    + "INDEX economy_journal_at (recorded_at), "
                    + "INDEX economy_journal_ic_week (ic_year, ic_week), "
                    + "INDEX economy_journal_ic_festival (ic_year, ic_festival), "
                    + "INDEX economy_journal_kind (kind, recorded_at), "
                    + "INDEX economy_journal_business (business_id, recorded_at), "
                    + "INDEX economy_journal_pier (pier_id, recorded_at), "
                    + "INDEX economy_journal_subject (subject_id), "
                    + "INDEX economy_journal_actor (actor_uuid, recorded_at)) ENGINE=InnoDB");

    private DatabaseSchema() {
    }

    /** Columns added after their table first shipped: table, column, definition. */
    private static final List<String[]> ADDED_COLUMNS = List.<String[]>of(
            new String[] {"price_ledger", "expression", "VARCHAR(255) NULL"},
            // percent or flat; rows from before flat amounts existed are percentages.
            new String[] {"price_modifiers", "kind", "VARCHAR(16) NOT NULL DEFAULT 'percent'"},
            // Who sold a Claim: added with player resale; earlier sales were all server sales.
            new String[] {"claim_sales", "seller", "TEXT NULL"},
            new String[] {"claim_sales", "seller_paid_later", "BOOLEAN NOT NULL DEFAULT FALSE"},
            // The receiving pier picked for an import, and its tags; NULL / empty for imports from before piers.
            new String[] {"business_deliveries", "pier_id", "VARCHAR(64) NULL"},
            new String[] {"business_deliveries", "tags", "VARCHAR(255) NOT NULL DEFAULT ''"},
            // Whether calendar node modifiers apply; every node from before them does.
            new String[] {"nodes", "modifiers_enabled", "BOOLEAN NOT NULL DEFAULT TRUE"});

    /**
     * Columns that became optional after their table first shipped: table, column, new
     * definition. The business owner columns are empty for server-owned businesses.
     */
    private static final List<String[]> RELAXED_COLUMNS = List.<String[]>of(
            new String[] {"businesses", "owner_uuid", "CHAR(36) NULL"},
            new String[] {"businesses", "owner_profile", "VARCHAR(64) NULL"});

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
        for (String[] column : RELAXED_COLUMNS) {
            makeNullable(connection, column[0], column[1], column[2]);
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

    /** Lets a NOT NULL column hold NULL; does nothing once it already can, so restarts stay cheap. */
    private static void makeNullable(Connection connection, String table, String column, String definition)
            throws SQLException {
        try (PreparedStatement check = connection.prepareStatement("SELECT IS_NULLABLE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            check.setString(1, table);
            check.setString(2, column);
            try (ResultSet result = check.executeQuery()) {
                if (!result.next() || result.getString(1).equalsIgnoreCase("YES")) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " " + definition);
        }
    }
}
