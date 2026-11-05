package org.skysongdev.skysongEconomy.data.database;

import org.bukkit.Bukkit;
import org.skysongdev.skysongEconomy.data.datatypes.DeliveringOffer;
import org.skysongdev.skysongEconomy.data.datatypes.EconomyBusiness;
import org.skysongdev.skysongEconomy.utils.Utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private final String HOST;
    private final String PORT;
    private final String USER;
    private final String PASSWORD;
    private final String DATABASE_NAME;

    public Database(String HOST, String PORT, String USER, String PASSWORD, String DATABASE_NAME){
        this.HOST = HOST;
        this.PORT = PORT;
        this.USER = USER;
        this.PASSWORD = PASSWORD;
        this.DATABASE_NAME = DATABASE_NAME;
    }

    private Connection sqlConnection;

    //Initialization
    public Connection getConnection() throws SQLException {
        // Reuse the cached connection only while it is still alive; MySQL drops idle connections.
        if(sqlConnection != null && !sqlConnection.isClosed() && sqlConnection.isValid(2)){
            return sqlConnection;
        }
        String url = "jdbc:mysql://" + this.HOST + ":" + this.PORT + "/" + this.DATABASE_NAME;
        sqlConnection = DriverManager.getConnection(url, this.USER, this.PASSWORD);
        Bukkit.getLogger().info("Connected to the database");
        return sqlConnection;
    }

    public void close() {
        if(sqlConnection == null){
            return;
        }
        try {
            sqlConnection.close();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[SkysongEconomy] Failed to close the database connection: " + e.getMessage());
        }
        sqlConnection = null;
    }

    public void initializeDatabase() throws SQLException {
        try (Statement buildStatement = getConnection().createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS price_ledger (" +
                    "materialId VARCHAR(100) PRIMARY KEY NOT NULL," +
                    "cost FLOAT DEFAULT 0)";
            buildStatement.execute(sql);
            String goodTypeSql = "CREATE TABLE IF NOT EXISTS good_types (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, goodType varchar(100)," +
                    "materialId varchar(100))";
            buildStatement.execute(goodTypeSql);
            String importTableSql = "CREATE TABLE IF NOT EXISTS import_slots(" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, business_id varchar(100), slot_id INT," +
                    "import_id varchar(100), arrival_date DATETIME)";
            buildStatement.execute(importTableSql);
            String econBusinessTableSql = "CREATE TABLE IF NOT EXISTS economy_businesses(" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, business_id varchar(100)," +
                    "owner_uuid varchar(100), business_name varchar(100), member_uuids varchar(5000), catalog_ids varchar(5000), x INT, y INT, z INT, world VARCHAR(255))";
            buildStatement.execute(econBusinessTableSql);
        }
    }

    //Ledger Table Functions
    public void createLedgerData(String materialId, float value) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("INSERT INTO price_ledger(materialId, cost) VALUES (?, ?)")) {
            statement.setString(1, materialId);
            statement.setFloat(2, value);
            statement.executeUpdate();
        }
    }

    public void updateLedgerData(String materialId, float value) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("UPDATE price_ledger SET cost = ? WHERE materialId = ?")) {
            statement.setFloat(1, value);
            statement.setString(2, materialId);
            statement.executeUpdate();
        }
    }

    public void deleteLedgerData(String materialId) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("DELETE from price_ledger WHERE materialId = ?")) {
            statement.setString(1, materialId);
            statement.executeUpdate();
        }
    }

    public float findItemCost(String materialId) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("SELECT cost FROM price_ledger WHERE materialId = ?")) {
            statement.setString(1, materialId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? results.getFloat("cost") : -1;
            }
        }
    }

    //Good Types Table Functions
    public void createGoodTypeAssociation(String goodType, String materialId) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("INSERT INTO good_types(goodType, materialId) VALUES (?, ?)")) {
            statement.setString(1, goodType);
            statement.setString(2, materialId);
            statement.executeUpdate();
        }
    }
    public void deleteGoodTypeAssociation(String goodType, String materialId) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("DELETE from good_types WHERE goodType = ? AND materialId = ?")) {
            statement.setString(1, goodType);
            statement.setString(2, materialId);
            statement.executeUpdate();
        }
    }
    public ArrayList<String> findAllGoodTypesFromMaterial(String materialId) throws SQLException {
        ArrayList<String> results = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement("SELECT goodType FROM good_types WHERE materialId = ?")) {
            statement.setString(1, materialId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while(resultSet.next()){
                    results.add(resultSet.getString("goodType"));
                }
            }
        }
        return results;
    }
    public boolean isMaterialInGoodType(String materialId, String goodType) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("SELECT 1 FROM good_types WHERE materialId = ? AND goodType = ? LIMIT 1")) {
            statement.setString(1, materialId);
            statement.setString(2, goodType);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Import Slots Table Functions
    public void createImportSlot(String businessId, int slotId, String importId, Timestamp arrivalDate) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("INSERT INTO import_slots(business_id, slot_id, import_id, arrival_date) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, businessId);
            statement.setInt(2, slotId);
            statement.setString(3, importId);
            statement.setTimestamp(4, arrivalDate);
            statement.executeUpdate();
        }
    }
    public void updateImportSlot(String businessId, int slotId, String importId, Timestamp arrivalDate) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("UPDATE import_slots SET import_id = ?, arrival_date = ? WHERE business_id = ? AND slot_id = ?")) {
            statement.setString(1, importId);
            statement.setTimestamp(2, arrivalDate);
            statement.setString(3, businessId);
            statement.setInt(4, slotId);
            statement.executeUpdate();
        }
    }
    public void deleteImportSlot(String businessId, int slotId) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("DELETE from import_slots WHERE business_id = ? AND slot_id = ?")) {
            statement.setString(1, businessId);
            statement.setInt(2, slotId);
            statement.executeUpdate();
        }
    }

    // Economy Business Table Functions
    public void createEconomyBusinessData(EconomyBusiness business) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("INSERT INTO economy_businesses(business_id, owner_uuid, business_name, member_uuids, catalog_ids, x, y, z, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, business.getId());
            statement.setString(2, business.getOwnerUUID());
            statement.setString(3, business.getName());
            statement.setString(4, String.join(";", business.getMemberUUIDs()));
            statement.setString(5, String.join(";", business.getCatalogIds()));
            statement.setInt(6, business.getLocation().get(0));
            statement.setInt(7, business.getLocation().get(1));
            statement.setInt(8, business.getLocation().get(2));
            statement.setString(9, business.getWorld());
            statement.executeUpdate();
        }
    }

    public void updateEconomyBusinessData(EconomyBusiness business) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("UPDATE economy_businesses SET owner_uuid = ?, business_name = ?, member_uuids = ?, catalog_ids = ?, x = ?, y = ?, z = ?, world = ? WHERE business_id = ?")) {
            statement.setString(1, business.getOwnerUUID());
            statement.setString(2, business.getName());
            statement.setString(3, String.join(";", business.getMemberUUIDs()));
            statement.setString(4, String.join(";", business.getCatalogIds()));
            statement.setInt(5, business.getLocation().get(0));
            statement.setInt(6, business.getLocation().get(1));
            statement.setInt(7, business.getLocation().get(2));
            statement.setString(8, business.getWorld());
            statement.setString(9, business.getId());
            statement.executeUpdate();
        }
    }

    public void deleteEconomyBusinessData(String businessId) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("DELETE from economy_businesses WHERE business_id = ?")) {
            statement.setString(1, businessId);
            statement.executeUpdate();
        }
    }

    //Dump Functions
    public void dumpEconomyBusinessData() throws SQLException{
        try (Statement statement = getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM economy_businesses")) {
            while(resultSet.next()){
                String businessId = resultSet.getString("business_id");
                String ownerUUID = resultSet.getString("owner_uuid");
                String businessName = resultSet.getString("business_name");
                String memberUUIDs = resultSet.getString("member_uuids");
                String catalogIDs = resultSet.getString("catalog_ids");
                int x = resultSet.getInt("x");
                int y = resultSet.getInt("y");
                int z = resultSet.getInt("z");
                String world = resultSet.getString("world");

                Utils.economyBusinesses.put(businessId, new EconomyBusiness(businessId, world, businessName, ownerUUID, new ArrayList<String>(List.of(catalogIDs.split(";"))), new ArrayList<String>(List.of(memberUUIDs.split(";"))), new ArrayList<Integer>(List.of(x, y, z)), new ArrayList<DeliveringOffer>()));

                Bukkit.getLogger().info("Business ID: " + businessId + ", Owner UUID: " + ownerUUID + ", Business Name: " + businessName +
                        ", Member UUIDs: " + memberUUIDs + ", Catalog IDs: " + catalogIDs +
                        ", Location: (" + x + ", " + y + ", " + z + ") in World: " + world);
            }
        }
    }

    public void dumpImportSlotData() throws SQLException{
        try (Statement statement = getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM import_slots")) {
            while(resultSet.next()){
                String businessId = resultSet.getString("business_id");
                int slotId = resultSet.getInt("slot_id");
                String importId = resultSet.getString("import_id");
                Timestamp arrivalDate = resultSet.getTimestamp("arrival_date");

                EconomyBusiness business = Utils.economyBusinesses.get(businessId);
                if(business != null){
                    business.getImportSlots().set(slotId, new DeliveringOffer(importId, arrivalDate));
                }
                Bukkit.getLogger().info("Business ID: " + businessId + ", Slot ID: " + slotId +
                        ", Import ID: " + importId + ", Arrival Date: " + arrivalDate.toString());
            }
        }
    }
}
