package org.skysongdev.skysongEconomy.data.datatypes;

import java.util.ArrayList;

public class EconomyBusiness {
    private static String id;
    private static String world;
    private static String name;
    private static String ownerUUID;
    private static ArrayList<String> catalogIds;
    private static ArrayList<String> memberUUIDs;
    private static ArrayList<Integer> location;
    private static ArrayList<DeliveringOffer> importSlots;

    public static String getOwnerUUID() {
        return ownerUUID;
    }

    public static void setOwnerUUID(String ownerUUID) {
        EconomyBusiness.ownerUUID = ownerUUID;
    }

    public static ArrayList<String> getMemberUUIDs() {
        return memberUUIDs;
    }

    public static void setMemberUUIDs(ArrayList<String> memberUUIDs) {
        EconomyBusiness.memberUUIDs = memberUUIDs;
    }

    public static ArrayList<String> getCatalogIds() {
        return catalogIds;
    }

    public static void setCatalogIds(ArrayList<String> catalogIds) {
        EconomyBusiness.catalogIds = catalogIds;
    }

    public EconomyBusiness(String id, String world, String name, String ownerUUID, ArrayList<String> catalogIds, ArrayList<String> memberUUIDs, ArrayList<Integer> location, ArrayList<DeliveringOffer> importSlots) {
        this.id = id;
        this.world = world;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.catalogIds = catalogIds;
        this.memberUUIDs = memberUUIDs;
        this.location = location;
        this.importSlots = importSlots;
    }

    public static String getId() {
        return id;
    }

    public static void setId(String id) {
        EconomyBusiness.id = id;
    }

    public static String getWorld() {
        return world;
    }

    public static void setWorld(String world) {
        EconomyBusiness.world = world;
    }

    public static String getName() {
        return name;
    }

    public static void setName(String name) {
        EconomyBusiness.name = name;
    }

    public static ArrayList<Integer> getLocation() {
        return location;
    }

    public static void setLocation(ArrayList<Integer> location) {
        EconomyBusiness.location = location;
    }

    public static ArrayList<DeliveringOffer> getImportSlots() {
        return importSlots;
    }

    public static void setImportSlots(ArrayList<DeliveringOffer> importSlots) {
        EconomyBusiness.importSlots = importSlots;
    }
}
