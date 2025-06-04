package org.djtmk.chunkcollect.data;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a chunk collector with its properties and settings.
 */
public class CollectorData {
    private UUID owner;
    private Location blockLocation;
    private Chunk chunk;
    private int speedLevel;
    private int rangeLevel;
    private List<Material> filterList;
    private List<Location> linkedChests;
    private int maxLinkedChests;

    /**
     * Creates a new collector with default settings.
     *
     * @param owner The UUID of the player who owns this collector
     * @param blockLocation The location of the collector block
     * @param chunk The chunk this collector is responsible for
     */
    public CollectorData(UUID owner, Location blockLocation, Chunk chunk) {
        this.owner = owner;
        this.blockLocation = blockLocation;
        this.chunk = chunk;
        this.speedLevel = 0;
        this.rangeLevel = 0;
        this.filterList = List.of();
        this.linkedChests = new ArrayList<>();
        this.maxLinkedChests = 1; // Default to 1 linked chest
    }

    // Getters and setters
    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Location getBlockLocation() {
        return blockLocation;
    }

    public void setBlockLocation(Location blockLocation) {
        this.blockLocation = blockLocation;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public void setSpeedLevel(int speedLevel) {
        this.speedLevel = speedLevel;
    }

    public int getRangeLevel() {
        return rangeLevel;
    }

    public void setRangeLevel(int rangeLevel) {
        this.rangeLevel = rangeLevel;
    }

    public List<Material> getFilterList() {
        return filterList;
    }

    public void setFilterList(List<Material> filterList) {
        this.filterList = filterList;
    }

    public List<Location> getLinkedChests() {
        return linkedChests;
    }

    public void setLinkedChests(List<Location> linkedChests) {
        this.linkedChests = linkedChests;
    }

    public void addLinkedChest(Location chestLocation) {
        if (linkedChests.size() < maxLinkedChests) {
            linkedChests.add(chestLocation);
        }
    }

    public boolean removeLinkedChest(Location chestLocation) {
        return linkedChests.remove(chestLocation);
    }

    public int getMaxLinkedChests() {
        return maxLinkedChests;
    }

    public void setMaxLinkedChests(int maxLinkedChests) {
        this.maxLinkedChests = maxLinkedChests;
    }
}
