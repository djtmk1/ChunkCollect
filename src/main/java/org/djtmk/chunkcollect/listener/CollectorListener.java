package org.djtmk.chunkcollect.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.djtmk.chunkcollect.ChunkCollect;
import org.djtmk.chunkcollect.config.Config;
import org.djtmk.chunkcollect.manager.CollectorManager;

/**
 * Listens for events related to collectors.
 */
public class CollectorListener implements Listener {
    private final ChunkCollect plugin;
    private final CollectorManager collectorManager;
    private final Config config;

    /**
     * Creates a new collector listener.
     *
     * @param plugin The plugin instance
     * @param collectorManager The collector manager
     * @param config The plugin configuration
     */
    public CollectorListener(ChunkCollect plugin, CollectorManager collectorManager, Config config) {
        this.plugin = plugin;
        this.collectorManager = collectorManager;
        this.config = config;
    }

    /**
     * Handles block placement events.
     *
     * @param event The block place event
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Check if the block is a collector block
        if (block.getType() != config.getCollectorBlockType()) {
            return;
        }

        // Check if the player has permission to place collectors
        if (!player.hasPermission("chunkcollect.use")) {
            player.sendMessage(config.getMessage("no-permission"));
            event.setCancelled(true);
            return;
        }

        // Try to create a collector
        boolean created = collectorManager.createCollector(player, block);
        if (!created) {
            player.sendMessage(config.getMessage("max-collectors-reached"));
            event.setCancelled(true);
            return;
        }

        player.sendMessage(config.getMessage("collector-placed"));
    }

    /**
     * Handles block break events.
     *
     * @param event The block break event
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Check if the block is a collector
        if (!collectorManager.isCollector(block)) {
            return;
        }

        // Remove the collector
        boolean removed = collectorManager.removeCollector(block);
        if (removed) {
            player.sendMessage(config.getMessage("collector-removed"));
        }
    }

    /**
     * Handles player interaction events.
     *
     * @param event The player interact event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right clicks on blocks with the main hand
        if (event.getAction().isLeftClick() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();

        // Check if the player is in chest linking mode
        if (collectorManager.isPlayerLinkingChests(player)) {
            // Check if the block is a chest or trapped chest
            if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                // Get the collector ID
                String collectorId = collectorManager.getPlayerLinkingCollectorId(player);
                if (collectorId == null) {
                    // Something went wrong, cancel linking mode
                    collectorManager.stopChestLinking(player);
                    player.sendMessage(config.getMessage("chest-linking-cancelled"));
                    event.setCancelled(true);
                    return;
                }

                // Get the collector
                org.djtmk.chunkcollect.data.CollectorData collector = collectorManager.getAllCollectors().get(collectorId);
                if (collector == null) {
                    // Collector not found, cancel linking mode
                    collectorManager.stopChestLinking(player);
                    player.sendMessage(config.getMessage("chest-linking-cancelled"));
                    event.setCancelled(true);
                    return;
                }

                // Check if the chest is already linked to a collector
                if (collectorManager.isChestLinked(block)) {
                    player.sendMessage(config.getMessage("chest-already-linked"));
                    event.setCancelled(true);
                    return;
                }

                // Link the chest to the collector
                boolean linked = collectorManager.linkChest(collector, block);

                // Remove the player from linking mode
                collectorManager.stopChestLinking(player);

                if (linked) {
                    player.sendMessage(config.getMessage("chest-linked"));
                } else {
                    player.sendMessage(config.getMessage("max-linked-chests-reached"));
                }

                event.setCancelled(true);
                return;
            } else {
                // Not a chest, cancel linking mode
                collectorManager.stopChestLinking(player);
                player.sendMessage(config.getMessage("chest-linking-cancelled"));
                event.setCancelled(true);
                return;
            }
        }

        // Check if the block is a collector
        if (!collectorManager.isCollector(block)) {
            return;
        }

        // Check if the player has permission to use collectors
        if (!player.hasPermission("chunkcollect.use")) {
            player.sendMessage(config.getMessage("no-permission"));
            event.setCancelled(true);
            return;
        }

        // Get the collector
        org.djtmk.chunkcollect.data.CollectorData collector = collectorManager.getCollector(block);
        if (collector == null) {
            return;
        }

        // Open the collector GUI
        plugin.getServer().getPluginManager().callEvent(new org.bukkit.event.player.PlayerCommandPreprocessEvent(player, "/cc gui"));
        event.setCancelled(true);
    }
}
