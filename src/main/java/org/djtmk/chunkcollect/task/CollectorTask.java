package org.djtmk.chunkcollect.task;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.djtmk.chunkcollect.ChunkCollect;
import org.djtmk.chunkcollect.config.Config;
import org.djtmk.chunkcollect.data.CollectorData;
import org.djtmk.chunkcollect.manager.CollectorManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Task that collects items in chunks with collectors.
 */
public class CollectorTask extends BukkitRunnable {
    private final ChunkCollect plugin;
    private final CollectorManager collectorManager;
    private final Config config;

    // Cache of items that couldn't be stored
    private final Map<String, List<ItemStack>> pendingItems = new HashMap<>();

    /**
     * Creates a new collector task.
     *
     * @param plugin The plugin instance
     * @param collectorManager The collector manager
     * @param config The plugin configuration
     */
    public CollectorTask(ChunkCollect plugin, CollectorManager collectorManager, Config config) {
        this.plugin = plugin;
        this.collectorManager = collectorManager;
        this.config = config;
    }

    @Override
    public void run() {
        // Process all collectors
        for (Map.Entry<String, CollectorData> entry : collectorManager.getAllCollectors().entrySet()) {
            String collectorId = entry.getKey();
            CollectorData collector = entry.getValue();

            // Skip collectors in unloaded chunks
            if (!collector.getChunk().isLoaded()) {
                continue;
            }

            // Calculate collection interval based on speed level
            int interval = config.getDefaultCollectionInterval() - (collector.getSpeedLevel() * 2);
            if (interval < 1) {
                interval = 1;
            }

            // Only collect items every N ticks based on the interval
            if (plugin.getServer().getCurrentTick() % interval != 0) {
                continue;
            }

            // Collect items in the chunk
            collectItems(collectorId, collector);
        }
    }

    /**
     * Collects items in a chunk.
     *
     * @param collectorId The collector ID
     * @param collector The collector data
     */
    private void collectItems(String collectorId, CollectorData collector) {
        Chunk chunk = collector.getChunk();
        World world = chunk.getWorld();

        // Calculate collection range based on range level
        int range = collector.getRangeLevel();

        // Get all items in the chunk (or in range if range level > 0)
        List<Item> items = new ArrayList<>();
        if (range > 0) {
            // Get items in a radius around the collector
            Location center = collector.getBlockLocation();
            int chunkRadius = range / 16 + 1; // Convert blocks to chunks

            for (Entity entity : world.getNearbyEntities(center, range, range, range)) {
                if (entity instanceof Item) {
                    items.add((Item) entity);
                }
            }
        } else {
            // Get items only in this chunk
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof Item) {
                    items.add((Item) entity);
                }
            }
        }

        // No items to collect
        if (items.isEmpty()) {
            return;
        }

        // Get pending items for this collector
        List<ItemStack> pending = pendingItems.computeIfAbsent(collectorId, k -> new ArrayList<>());

        // Get linked chests
        List<Container> linkedContainers = getLinkedContainers(collector);

        // Process items
        for (Item item : items) {
            ItemStack stack = item.getItemStack();

            // Check if the item is in the filter list
            if (!isItemAllowed(collector, stack.getType())) {
                continue;
            }

            // Try to store the item in linked chests
            boolean stored = false;
            if (!linkedContainers.isEmpty()) {
                // Try each linked chest
                for (Container container : linkedContainers) {
                    HashMap<Integer, ItemStack> remaining = container.getInventory().addItem(stack);

                    if (remaining.isEmpty()) {
                        // Item was fully stored
                        stored = true;
                        break;
                    } else {
                        // Update the stack to the remaining amount
                        stack = remaining.values().iterator().next();
                    }
                }

                // If the item wasn't fully stored, add the remainder to pending
                if (!stored && stack != null && stack.getAmount() > 0) {
                    pending.add(stack);
                }
            } else {
                // No linked chests, add to pending
                pending.add(stack);
            }

            // Remove the item from the world
            item.remove();
        }

        // Try to process pending items if we have linked chests
        if (!linkedContainers.isEmpty() && !pending.isEmpty()) {
            List<ItemStack> stillPending = new ArrayList<>();

            for (ItemStack stack : pending) {
                boolean stored = false;

                // Try each linked chest
                for (Container container : linkedContainers) {
                    HashMap<Integer, ItemStack> remaining = container.getInventory().addItem(stack);

                    if (remaining.isEmpty()) {
                        // Item was fully stored
                        stored = true;
                        break;
                    } else {
                        // Update the stack to the remaining amount
                        stack = remaining.values().iterator().next();
                    }
                }

                // If the item wasn't fully stored, add the remainder to still pending
                if (!stored && stack != null && stack.getAmount() > 0) {
                    stillPending.add(stack);
                }
            }

            // Update pending items
            pending.clear();
            pending.addAll(stillPending);
        }

        // Update pending items map
        pendingItems.put(collectorId, pending);
    }

    /**
     * Gets all linked containers for a collector.
     *
     * @param collector The collector data
     * @return A list of linked containers
     */
    private List<Container> getLinkedContainers(CollectorData collector) {
        List<Container> containers = new ArrayList<>();

        // Get linked chest locations
        List<Location> linkedChests = collector.getLinkedChests();

        // Convert locations to containers
        for (Location location : linkedChests) {
            Block block = location.getBlock();
            BlockState state = block.getState();

            if (state instanceof Container) {
                containers.add((Container) state);
            }
        }

        // If no linked chests, try to find a nearby container
        if (containers.isEmpty()) {
            Container nearestContainer = findNearestContainer(collector);
            if (nearestContainer != null) {
                containers.add(nearestContainer);
            }
        }

        return containers;
    }

    /**
     * Finds the nearest container to a collector.
     *
     * @param collector The collector data
     * @return The nearest container, or null if none is found
     */
    private Container findNearestContainer(CollectorData collector) {
        Location location = collector.getBlockLocation();
        World world = location.getWorld();
        int range = config.getDefaultStorageRange();

        // Check nearby blocks for containers
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    Block block = world.getBlockAt(
                            location.getBlockX() + x,
                            location.getBlockY() + y,
                            location.getBlockZ() + z
                    );

                    BlockState state = block.getState();
                    if (state instanceof Container) {
                        return (Container) state;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if an item is allowed by the collector's filter.
     *
     * @param collector The collector data
     * @param material The material to check
     * @return true if the item is allowed, false otherwise
     */
    private boolean isItemAllowed(CollectorData collector, Material material) {
        // If filters are disabled, allow all items
        if (!config.isFiltersEnabled()) {
            return true;
        }

        List<Material> filterList = collector.getFilterList();

        // If the filter list is empty, allow all items
        if (filterList.isEmpty()) {
            return true;
        }

        // Check if the material is in the filter list
        return filterList.contains(material);
    }
}
