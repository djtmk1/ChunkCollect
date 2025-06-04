package org.djtmk.chunkcollect.manager;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.djtmk.chunkcollect.ChunkCollect;
import org.djtmk.chunkcollect.config.Config;
import org.djtmk.chunkcollect.data.CollectorData;
import org.djtmk.chunkcollect.database.DatabaseManager;
import org.djtmk.chunkcollect.database.SQLiteManager;
import org.djtmk.chunkcollect.database.MySQLManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all collectors in the server.
 */
public class CollectorManager {
    private final ChunkCollect plugin;
    private final Config config;
    private final Map<String, CollectorData> collectors = new ConcurrentHashMap<>();
    private final NamespacedKey collectorKey;
    private final NamespacedKey linkedChestKey;
    private final DatabaseManager databaseManager;

    // Map to track players in chest linking mode
    private final Map<UUID, String> playersLinkingChests = new HashMap<>();

    /**
     * Creates a new collector manager.
     *
     * @param plugin The plugin instance
     * @param config The plugin configuration
     */
    public CollectorManager(ChunkCollect plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.collectorKey = new NamespacedKey(plugin, "collector_id");
        this.linkedChestKey = new NamespacedKey(plugin, "linked_collector_id");

        // Initialize database manager based on configuration
        String dbType = config.getDatabaseType();
        if (dbType.equals("MYSQL")) {
            this.databaseManager = new MySQLManager(plugin, config);
            plugin.getLogger().info("Using MySQL database");
        } else {
            this.databaseManager = new SQLiteManager(plugin);
            plugin.getLogger().info("Using SQLite database");
        }

        if (!this.databaseManager.initialize()) {
            plugin.getLogger().severe("Failed to initialize database. Collectors will not be saved or loaded.");
        }

        loadCollectors();
    }

    /**
     * Loads all collectors from storage.
     */
    public void loadCollectors() {
        collectors.clear();

        // Load collectors from database
        Map<String, CollectorData> loadedCollectors = databaseManager.loadCollectors();
        if (loadedCollectors != null && !loadedCollectors.isEmpty()) {
            collectors.putAll(loadedCollectors);
            plugin.getLogger().info("Loaded " + collectors.size() + " collectors from database.");
        } else {
            plugin.getLogger().info("No collectors found in database.");
        }
    }

    /**
     * Saves all collectors to storage.
     */
    public void saveCollectors() {
        // Save collectors to database
        if (databaseManager.saveCollectors(collectors)) {
            plugin.getLogger().info("Saved " + collectors.size() + " collectors to database.");
        } else {
            plugin.getLogger().warning("Failed to save collectors to database.");
        }
    }

    /**
     * Closes the database connection.
     * This should be called when the plugin is disabled.
     */
    public void closeDatabase() {
        databaseManager.close();
        plugin.getLogger().info("Database connection closed.");
    }

    /**
     * Creates a new collector at the specified location.
     *
     * @param player The player creating the collector
     * @param block The block to use as a collector
     * @return true if the collector was created, false otherwise
     */
    public boolean createCollector(Player player, Block block) {
        Chunk chunk = block.getChunk();
        String chunkKey = getChunkKey(chunk);

        // Check if there's already a collector in this chunk
        if (hasCollectorInChunk(chunk) && !player.hasPermission("chunkcollect.bypass.limit")) {
            return false;
        }

        // Create a new collector
        String collectorId = UUID.randomUUID().toString();
        CollectorData collector = new CollectorData(player.getUniqueId(), block.getLocation(), chunk);

        // Set the maximum number of linked chests based on configuration
        collector.setMaxLinkedChests(config.getMaxManualLinkedChests());

        collectors.put(collectorId, collector);

        // Mark the block as a collector
        BlockState state = block.getState();
        if (!(state instanceof TileState)) {
            plugin.getLogger().warning("Block at " + block.getLocation() + " is not a tile entity and cannot store data!");
            return false;
        }

        TileState tileState = (TileState) state;
        PersistentDataContainer blockData = tileState.getPersistentDataContainer();
        blockData.set(collectorKey, PersistentDataType.STRING, collectorId);
        tileState.update();

        // Auto-link chests if enabled
        if (config.isAutoLinkingEnabled() && config.isAutoLinkOnPlacement()) {
            int linkedCount = autoLinkChests(collector);
            if (linkedCount > 0) {
                player.sendMessage(ChatColor.GREEN + "Auto-linked " + linkedCount + " chest" + (linkedCount > 1 ? "s" : "") + " to your collector.");
            }
        }

        // Save the collectors
        saveCollectors();

        return true;
    }

    /**
     * Removes a collector at the specified location.
     *
     * @param block The collector block
     * @return true if the collector was removed, false if no collector was found
     */
    public boolean removeCollector(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof TileState)) {
            return false;
        }

        TileState tileState = (TileState) state;
        PersistentDataContainer blockData = tileState.getPersistentDataContainer();
        if (!blockData.has(collectorKey, PersistentDataType.STRING)) {
            return false;
        }

        String collectorId = blockData.get(collectorKey, PersistentDataType.STRING);
        if (collectorId == null || !collectors.containsKey(collectorId)) {
            return false;
        }

        // Remove the collector
        collectors.remove(collectorId);

        // Remove the block data
        blockData.remove(collectorKey);
        tileState.update();

        // Save the collectors
        saveCollectors();

        return true;
    }

    /**
     * Checks if a block is a collector.
     *
     * @param block The block to check
     * @return true if the block is a collector, false otherwise
     */
    public boolean isCollector(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof TileState)) {
            return false;
        }

        TileState tileState = (TileState) state;
        PersistentDataContainer blockData = tileState.getPersistentDataContainer();
        return blockData.has(collectorKey, PersistentDataType.STRING);
    }

    /**
     * Gets the collector at the specified location.
     *
     * @param block The collector block
     * @return The collector data, or null if no collector was found
     */
    public CollectorData getCollector(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof TileState)) {
            return null;
        }

        TileState tileState = (TileState) state;
        PersistentDataContainer blockData = tileState.getPersistentDataContainer();
        if (!blockData.has(collectorKey, PersistentDataType.STRING)) {
            return null;
        }

        String collectorId = blockData.get(collectorKey, PersistentDataType.STRING);
        return collectors.get(collectorId);
    }

    /**
     * Checks if a chunk has a collector.
     *
     * @param chunk The chunk to check
     * @return true if the chunk has a collector, false otherwise
     */
    public boolean hasCollectorInChunk(Chunk chunk) {
        String chunkKey = getChunkKey(chunk);
        for (CollectorData collector : collectors.values()) {
            if (getChunkKey(collector.getChunk()).equals(chunkKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all collectors.
     *
     * @return A map of collector IDs to collector data
     */
    public Map<String, CollectorData> getAllCollectors() {
        return new HashMap<>(collectors);
    }

    /**
     * Gets a unique key for a chunk.
     *
     * @param chunk The chunk
     * @return A unique key for the chunk
     */
    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    /**
     * Links a chest to a collector.
     *
     * @param collector The collector data
     * @param chest The chest block
     * @return true if the chest was linked, false otherwise
     */
    public boolean linkChest(CollectorData collector, Block chest) {
        // Check if the block is a chest
        if (!(chest.getState() instanceof Container)) {
            return false;
        }

        // Check if the chest is already linked to a collector
        if (isChestLinked(chest)) {
            return false;
        }

        // Check if the collector has reached the maximum number of linked chests
        if (collector.getLinkedChests().size() >= collector.getMaxLinkedChests()) {
            return false;
        }

        // Link the chest to the collector
        collector.addLinkedChest(chest.getLocation());

        // Mark the chest as linked
        BlockState state = chest.getState();
        if (state instanceof TileState) {
            TileState tileState = (TileState) state;
            PersistentDataContainer blockData = tileState.getPersistentDataContainer();

            // Get the collector ID
            String collectorId = null;
            for (Map.Entry<String, CollectorData> entry : collectors.entrySet()) {
                if (entry.getValue() == collector) {
                    collectorId = entry.getKey();
                    break;
                }
            }

            if (collectorId != null) {
                blockData.set(linkedChestKey, PersistentDataType.STRING, collectorId);
                tileState.update();
            }
        }

        // Save the collectors
        saveCollectors();

        return true;
    }

    /**
     * Unlinks a chest from a collector.
     *
     * @param chest The chest block
     * @return true if the chest was unlinked, false otherwise
     */
    public boolean unlinkChest(Block chest) {
        // Check if the chest is linked to a collector
        String collectorId = getLinkedCollectorId(chest);
        if (collectorId == null) {
            return false;
        }

        // Get the collector
        CollectorData collector = collectors.get(collectorId);
        if (collector == null) {
            return false;
        }

        // Unlink the chest from the collector
        boolean removed = collector.removeLinkedChest(chest.getLocation());
        if (!removed) {
            return false;
        }

        // Remove the link from the chest
        BlockState state = chest.getState();
        if (state instanceof TileState) {
            TileState tileState = (TileState) state;
            PersistentDataContainer blockData = tileState.getPersistentDataContainer();
            blockData.remove(linkedChestKey);
            tileState.update();
        }

        // Save the collectors
        saveCollectors();

        return true;
    }

    /**
     * Checks if a chest is linked to a collector.
     *
     * @param chest The chest block
     * @return true if the chest is linked, false otherwise
     */
    public boolean isChestLinked(Block chest) {
        return getLinkedCollectorId(chest) != null;
    }

    /**
     * Gets the ID of the collector that a chest is linked to.
     *
     * @param chest The chest block
     * @return The collector ID, or null if the chest is not linked
     */
    public String getLinkedCollectorId(Block chest) {
        BlockState state = chest.getState();
        if (!(state instanceof TileState)) {
            return null;
        }

        TileState tileState = (TileState) state;
        PersistentDataContainer blockData = tileState.getPersistentDataContainer();

        if (!blockData.has(linkedChestKey, PersistentDataType.STRING)) {
            return null;
        }

        return blockData.get(linkedChestKey, PersistentDataType.STRING);
    }

    /**
     * Auto-links nearby chests to a collector.
     *
     * @param collector The collector data
     * @return The number of chests that were linked
     */
    public int autoLinkChests(CollectorData collector) {
        if (!config.isAutoLinkingEnabled()) {
            return 0;
        }

        int linkedCount = 0;
        int maxAutoLinked = Math.min(config.getMaxAutoLinkedChests(), collector.getMaxLinkedChests());
        int range = config.getDefaultStorageRange();

        // If the collector already has linked chests, don't auto-link more
        if (!collector.getLinkedChests().isEmpty()) {
            return 0;
        }

        Location location = collector.getBlockLocation();
        World world = location.getWorld();

        // Check nearby blocks for chests
        for (int x = -range; x <= range && linkedCount < maxAutoLinked; x++) {
            for (int y = -range; y <= range && linkedCount < maxAutoLinked; y++) {
                for (int z = -range; z <= range && linkedCount < maxAutoLinked; z++) {
                    Block block = world.getBlockAt(
                            location.getBlockX() + x,
                            location.getBlockY() + y,
                            location.getBlockZ() + z
                    );

                    if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                        if (linkChest(collector, block)) {
                            linkedCount++;
                        }
                    }
                }
            }
        }

        return linkedCount;
    }

    /**
     * Puts a player in chest linking mode.
     *
     * @param player The player
     * @param collectorId The ID of the collector to link chests to
     */
    public void startChestLinking(Player player, String collectorId) {
        playersLinkingChests.put(player.getUniqueId(), collectorId);
    }

    /**
     * Removes a player from chest linking mode.
     *
     * @param player The player
     * @return The ID of the collector that the player was linking chests to, or null if the player was not in linking mode
     */
    public String stopChestLinking(Player player) {
        return playersLinkingChests.remove(player.getUniqueId());
    }

    /**
     * Checks if a player is in chest linking mode.
     *
     * @param player The player
     * @return true if the player is in linking mode, false otherwise
     */
    public boolean isPlayerLinkingChests(Player player) {
        return playersLinkingChests.containsKey(player.getUniqueId());
    }

    /**
     * Gets the ID of the collector that a player is linking chests to.
     *
     * @param player The player
     * @return The collector ID, or null if the player is not in linking mode
     */
    public String getPlayerLinkingCollectorId(Player player) {
        return playersLinkingChests.get(player.getUniqueId());
    }
}
