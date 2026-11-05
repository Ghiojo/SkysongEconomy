package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.data.datatypes.DeliveringOffer;
import org.skysongdev.skysongEconomy.data.datatypes.EconomyBusiness;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Economy businesses and the deliveries in their import slots
 * ({@code economy_businesses} and {@code import_slots} tables).
 */
public final class EconomyBusinessRepository {
    private static final String LIST_SEPARATOR = ";";

    private final ConnectionProvider connections;

    public EconomyBusinessRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    // Businesses

    public void create(EconomyBusiness business) throws SQLException {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO economy_businesses (owner_uuid, business_name, member_uuids, catalog_ids, x, y, z, world, business_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            bindBusiness(statement, business);
            statement.executeUpdate();
        }
    }

    public void update(EconomyBusiness business) throws SQLException {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "UPDATE economy_businesses SET owner_uuid = ?, business_name = ?, member_uuids = ?, catalog_ids = ?, "
                        + "x = ?, y = ?, z = ?, world = ? WHERE business_id = ?")) {
            bindBusiness(statement, business);
            statement.executeUpdate();
        }
    }

    /** Deletes the business and every delivery in its slots. */
    public void delete(String businessId) throws SQLException {
        try (PreparedStatement deleteSlots = connections.getConnection()
                     .prepareStatement("DELETE FROM import_slots WHERE business_id = ?");
             PreparedStatement deleteBusiness = connections.getConnection()
                     .prepareStatement("DELETE FROM economy_businesses WHERE business_id = ?")) {
            deleteSlots.setString(1, businessId);
            deleteSlots.executeUpdate();
            deleteBusiness.setString(1, businessId);
            deleteBusiness.executeUpdate();
        }
    }

    /** Loads every business with its import slots filled in. */
    public List<EconomyBusiness> findAll() throws SQLException {
        Map<String, EconomyBusiness> businesses = new LinkedHashMap<>();
        try (Statement statement = connections.getConnection().createStatement();
             ResultSet results = statement.executeQuery("SELECT * FROM economy_businesses")) {
            while (results.next()) {
                EconomyBusiness business = readBusiness(results);
                businesses.put(business.getId(), business);
            }
        }
        try (Statement statement = connections.getConnection().createStatement();
             ResultSet results = statement.executeQuery("SELECT business_id, slot_id, import_id, arrival_date FROM import_slots")) {
            while (results.next()) {
                EconomyBusiness business = businesses.get(results.getString("business_id"));
                if (business != null) {
                    putInSlot(business, results.getInt("slot_id"),
                            new DeliveringOffer(results.getString("import_id"), results.getTimestamp("arrival_date")));
                }
            }
        }
        return new ArrayList<>(businesses.values());
    }

    // Import slots

    /** Stores a delivery in a slot, replacing whatever the slot held. */
    public void saveSlot(String businessId, int slotId, DeliveringOffer delivery) throws SQLException {
        clearSlot(businessId, slotId);
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "INSERT INTO import_slots (business_id, slot_id, import_id, arrival_date) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, businessId);
            statement.setInt(2, slotId);
            statement.setString(3, delivery.getOfferName());
            statement.setTimestamp(4, delivery.getDeliveryDate());
            statement.executeUpdate();
        }
    }

    public void clearSlot(String businessId, int slotId) throws SQLException {
        try (PreparedStatement statement = connections.getConnection()
                .prepareStatement("DELETE FROM import_slots WHERE business_id = ? AND slot_id = ?")) {
            statement.setString(1, businessId);
            statement.setInt(2, slotId);
            statement.executeUpdate();
        }
    }

    // Mapping

    /** Binds the columns in the order shared by {@link #create} and {@link #update}: business_id last. */
    private static void bindBusiness(PreparedStatement statement, EconomyBusiness business) throws SQLException {
        statement.setString(1, business.getOwnerUUID());
        statement.setString(2, business.getName());
        statement.setString(3, String.join(LIST_SEPARATOR, business.getMemberUUIDs()));
        statement.setString(4, String.join(LIST_SEPARATOR, business.getCatalogIds()));
        statement.setInt(5, business.getLocation().get(0));
        statement.setInt(6, business.getLocation().get(1));
        statement.setInt(7, business.getLocation().get(2));
        statement.setString(8, business.getWorld());
        statement.setString(9, business.getId());
    }

    private static EconomyBusiness readBusiness(ResultSet row) throws SQLException {
        return new EconomyBusiness(
                row.getString("business_id"),
                row.getString("world"),
                row.getString("business_name"),
                row.getString("owner_uuid"),
                splitList(row.getString("catalog_ids")),
                splitList(row.getString("member_uuids")),
                new ArrayList<>(List.of(row.getInt("x"), row.getInt("y"), row.getInt("z"))),
                new ArrayList<>());
    }

    private static void putInSlot(EconomyBusiness business, int slotId, DeliveringOffer delivery) {
        List<DeliveringOffer> slots = business.getImportSlots();
        while (slots.size() <= slotId) {
            slots.add(null); // empty slot
        }
        slots.set(slotId, delivery);
    }

    /** Splits a ';'-joined column, treating null or empty as an empty list (not a list with one empty string). */
    private static ArrayList<String> splitList(String joined) {
        ArrayList<String> values = new ArrayList<>();
        if (joined != null && !joined.isEmpty()) {
            values.addAll(List.of(joined.split(LIST_SEPARATOR)));
        }
        return values;
    }
}
