package org.djtmk.chunkcollect.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.djtmk.chunkcollect.ChunkCollect;
import org.djtmk.chunkcollect.gui.CollectorGUI;

/**
 * Listens for GUI-related events.
 */
public class GUIListener implements Listener {
    private final ChunkCollect plugin;
    private final CollectorGUI collectorGUI;

    /**
     * Creates a new GUI listener.
     *
     * @param plugin The plugin instance
     * @param collectorGUI The collector GUI
     */
    public GUIListener(ChunkCollect plugin, CollectorGUI collectorGUI) {
        this.plugin = plugin;
        this.collectorGUI = collectorGUI;
    }

    /**
     * Handles inventory click events.
     *
     * @param event The inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Check if the player has an open GUI
        if (!collectorGUI.hasOpenGUI(player)) {
            return;
        }

        // Cancel the event to prevent item movement
        event.setCancelled(true);

        // Handle the click with click type
        collectorGUI.handleClick(player, event.getRawSlot(), event.getClick());
    }

    /**
     * Handles inventory close events.
     *
     * @param event The inventory close event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // Remove the player from the open GUIs cache
        collectorGUI.removePlayer(player);
    }
}
