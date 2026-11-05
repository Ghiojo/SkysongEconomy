package org.skysongdev.skysongEconomy.data.datatypes;

import java.util.ArrayList;

public class EconomyBusiness {
    private String id;
    private String world;
    private String name;
    private String ownerUUID;
    private ArrayList<String> catalogIds;
    private ArrayList<String> memberUUIDs;
    private ArrayList<Integer> location;
    private ArrayList<DeliveringOffer> importSlots;

    public String getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(String ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public ArrayList<String> getMemberUUIDs() {
        return memberUUIDs;
    }

    public void setMemberUUIDs(ArrayList<String> memberUUIDs) {
        this.memberUUIDs = memberUUIDs;
    }

    public ArrayList<String> getCatalogIds() {
        return catalogIds;
    }

    public void setCatalogIds(ArrayList<String> catalogIds) {
        this.catalogIds = catalogIds;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Integer> getLocation() {
        return location;
    }

    public void setLocation(ArrayList<Integer> location) {
        this.location = location;
    }

    public ArrayList<DeliveringOffer> getImportSlots() {
        return importSlots;
    }

    public void setImportSlots(ArrayList<DeliveringOffer> importSlots) {
        this.importSlots = importSlots;
    }
}
