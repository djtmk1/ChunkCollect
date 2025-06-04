package org.djtmk.chunkcollect.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.djtmk.chunkcollect.ChunkCollect;
import org.djtmk.chunkcollect.config.Config;
import org.djtmk.chunkcollect.data.CollectorData;
import org.djtmk.chunkcollect.manager.CollectorManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the collector GUI.
 */
public class CollectorGUI {
    private final ChunkCollect plugin;
    private final Config config;
    private final CollectorManager collectorManager;

    // Cache of open GUIs
    private final Map<Player, CollectorData> openGUIs = new HashMap<>();

    /**
     * Creates a new collector GUI manager.
     *
     * @param plugin The plugin instance
     * @param config The plugin configuration
     * @param collectorManager The collector manager
     */
    public CollectorGUI(ChunkCollect plugin, Config config, CollectorManager collectorManager) {
        this.plugin = plugin;
        this.config = config;
        this.collectorManager = collectorManager;
    }

    /**
     * Opens the collector GUI for a player.
     *
     * @param player The player
     * @param collector The collector data
     */
    public void openGUI(Player player, CollectorData collector) {
        Inventory inventory = createMainGUI(collector);
        player.openInventory(inventory);
        openGUIs.put(player, collector);
    }

    /**
     * Creates the main collector GUI.
     *
     * @param collector The collector data
     * @return The inventory
     */
    private Inventory createMainGUI(CollectorData collector) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Chunk Collector");

        // Stats item
        ItemStack statsItem = createItem(
                Material.COMPASS,
                ChatColor.GOLD + "Collector Stats",
                List.of(
                        ChatColor.GRAY + "Location: " + formatLocation(collector.getBlockLocation()),
                        ChatColor.GRAY + "Speed Level: " + collector.getSpeedLevel(),
                        ChatColor.GRAY + "Range Level: " + collector.getRangeLevel(),
                        ChatColor.GRAY + "Filters: " + (collector.getFilterList().isEmpty() ? "None" : collector.getFilterList().size() + " items"),
                        ChatColor.GRAY + "Linked Chests: " + collector.getLinkedChests().size() + "/" + collector.getMaxLinkedChests()
                )
        );
        inventory.setItem(4, statsItem);

        // Speed upgrade item
        ItemStack speedItem = createItem(
                Material.CLOCK,
                ChatColor.GREEN + "Speed Upgrade",
                List.of(
                        ChatColor.GRAY + "Current Level: " + collector.getSpeedLevel(),
                        ChatColor.GRAY + "Max Level: " + config.getConfig().getInt("upgrades.speed.max-level", 5),
                        ChatColor.GRAY + "Cost: " + config.getConfig().getInt("upgrades.speed.cost-per-level", 5) + " " + config.getConfig().getString("upgrades.speed.cost-type", "XP"),
                        "",
                        ChatColor.YELLOW + "Click to upgrade!"
                )
        );
        inventory.setItem(10, speedItem);

        // Range upgrade item
        ItemStack rangeItem = createItem(
                Material.ENDER_EYE,
                ChatColor.AQUA + "Range Upgrade",
                List.of(
                        ChatColor.GRAY + "Current Level: " + collector.getRangeLevel(),
                        ChatColor.GRAY + "Max Level: " + config.getConfig().getInt("upgrades.range.max-level", 3),
                        ChatColor.GRAY + "Cost: " + config.getConfig().getInt("upgrades.range.cost-per-level", 10) + " " + config.getConfig().getString("upgrades.range.cost-type", "XP"),
                        "",
                        ChatColor.YELLOW + "Click to upgrade!"
                )
        );
        inventory.setItem(12, rangeItem);

        // Filter item
        ItemStack filterItem = createItem(
                Material.HOPPER,
                ChatColor.LIGHT_PURPLE + "Item Filters",
                List.of(
                        ChatColor.GRAY + "Current Filters: " + (collector.getFilterList().isEmpty() ? "None" : collector.getFilterList().size() + " items"),
                        "",
                        ChatColor.YELLOW + "Click to configure filters!"
                )
        );
        inventory.setItem(14, filterItem);

        // Chest linking item
        List<String> chestLinkingLore = new ArrayList<>();
        chestLinkingLore.add(ChatColor.GRAY + "Linked Chests: " + collector.getLinkedChests().size() + "/" + collector.getMaxLinkedChests());
        chestLinkingLore.add("");

        if (collector.getLinkedChests().size() < collector.getMaxLinkedChests()) {
            chestLinkingLore.add(ChatColor.YELLOW + "Right-click to link a new chest");
        }

        if (!collector.getLinkedChests().isEmpty()) {
            chestLinkingLore.add(ChatColor.YELLOW + "Left-click to view linked chests");
        }

        ItemStack chestLinkingItem = createItem(
                Material.CHEST,
                ChatColor.GOLD + "Chest Linking",
                chestLinkingLore
        );
        inventory.setItem(16, chestLinkingItem);

        return inventory;
    }

    /**
     * Creates an item for the GUI.
     *
     * @param material The material
     * @param name The name
     * @param lore The lore
     * @return The item
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Formats a location for display.
     *
     * @param location The location
     * @return The formatted location
     */
    private String formatLocation(org.bukkit.Location location) {
        return location.getWorld().getName() + " (" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    /**
     * Handles a click in the GUI.
     *
     * @param player The player
     * @param slot The slot
     * @param clickType The type of click
     * @return true if the click was handled, false otherwise
     */
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        CollectorData collector = openGUIs.get(player);
        if (collector == null) {
            return false;
        }

        switch (slot) {
            case 10: // Speed upgrade
                upgradeSpeed(player, collector);
                return true;
            case 12: // Range upgrade
                upgradeRange(player, collector);
                return true;
            case 14: // Filter configuration
                openFilterGUI(player, collector);
                return true;
            case 16: // Chest linking
                if (clickType == ClickType.RIGHT) {
                    // Start chest linking mode
                    startChestLinking(player, collector);
                    return true;
                } else if (clickType == ClickType.LEFT) {
                    // View linked chests
                    openLinkedChestsGUI(player, collector);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    /**
     * Overloaded method for backward compatibility.
     *
     * @param player The player
     * @param slot The slot
     * @return true if the click was handled, false otherwise
     */
    public boolean handleClick(Player player, int slot) {
        return handleClick(player, slot, ClickType.LEFT);
    }

    /**
     * Upgrades the speed of a collector.
     *
     * @param player The player
     * @param collector The collector data
     */
    private void upgradeSpeed(Player player, CollectorData collector) {
        int currentLevel = collector.getSpeedLevel();
        int maxLevel = config.getConfig().getInt("upgrades.speed.max-level", 5);

        if (currentLevel >= maxLevel) {
            player.sendMessage(ChatColor.RED + "This collector is already at the maximum speed level!");
            return;
        }

        String costType = config.getConfig().getString("upgrades.speed.cost-type", "XP");
        int cost = config.getConfig().getInt("upgrades.speed.cost-per-level", 5);

        if (costType.equalsIgnoreCase("XP")) {
            if (player.getLevel() < cost) {
                player.sendMessage(ChatColor.RED + "You need " + cost + " XP levels to upgrade this collector!");
                return;
            }

            player.setLevel(player.getLevel() - cost);
        } else {
            // TODO: Implement other cost types (ITEM, ECONOMY)
            player.sendMessage(ChatColor.RED + "This upgrade cost type is not implemented yet!");
            return;
        }

        collector.setSpeedLevel(currentLevel + 1);
        player.sendMessage(ChatColor.GREEN + "Collector speed upgraded to level " + (currentLevel + 1) + "!");

        // Refresh the GUI
        openGUI(player, collector);
    }

    /**
     * Upgrades the range of a collector.
     *
     * @param player The player
     * @param collector The collector data
     */
    private void upgradeRange(Player player, CollectorData collector) {
        int currentLevel = collector.getRangeLevel();
        int maxLevel = config.getConfig().getInt("upgrades.range.max-level", 3);

        if (currentLevel >= maxLevel) {
            player.sendMessage(ChatColor.RED + "This collector is already at the maximum range level!");
            return;
        }

        String costType = config.getConfig().getString("upgrades.range.cost-type", "XP");
        int cost = config.getConfig().getInt("upgrades.range.cost-per-level", 10);

        if (costType.equalsIgnoreCase("XP")) {
            if (player.getLevel() < cost) {
                player.sendMessage(ChatColor.RED + "You need " + cost + " XP levels to upgrade this collector!");
                return;
            }

            player.setLevel(player.getLevel() - cost);
        } else {
            // TODO: Implement other cost types (ITEM, ECONOMY)
            player.sendMessage(ChatColor.RED + "This upgrade cost type is not implemented yet!");
            return;
        }

        collector.setRangeLevel(currentLevel + 1);
        player.sendMessage(ChatColor.GREEN + "Collector range upgraded to level " + (currentLevel + 1) + "!");

        // Refresh the GUI
        openGUI(player, collector);
    }

    /**
     * Opens the filter GUI for a player.
     *
     * @param player The player
     * @param collector The collector data
     */
    private void openFilterGUI(Player player, CollectorData collector) {
        // TODO: Implement filter GUI
        player.sendMessage(ChatColor.RED + "Filter GUI not implemented yet!");
    }

    /**
     * Starts chest linking mode for a player.
     *
     * @param player The player
     * @param collector The collector data
     */
    private void startChestLinking(Player player, CollectorData collector) {
        // Check if the collector has reached the maximum number of linked chests
        if (collector.getLinkedChests().size() >= collector.getMaxLinkedChests()) {
            player.sendMessage(config.getMessage("max-linked-chests-reached"));
            return;
        }

        // Get the collector ID
        String collectorId = null;
        for (Map.Entry<String, CollectorData> entry : collectorManager.getAllCollectors().entrySet()) {
            if (entry.getValue() == collector) {
                collectorId = entry.getKey();
                break;
            }
        }

        if (collectorId == null) {
            player.sendMessage(ChatColor.RED + "Failed to get collector ID.");
            return;
        }

        // Close the GUI
        player.closeInventory();

        // Put the player in chest linking mode
        collectorManager.startChestLinking(player, collectorId);

        // Send a message to the player
        player.sendMessage(config.getMessage("chest-linking-mode"));
    }

    /**
     * Opens the linked chests GUI for a player.
     *
     * @param player The player
     * @param collector The collector data
     */
    private void openLinkedChestsGUI(Player player, CollectorData collector) {
        // Check if the collector has any linked chests
        if (collector.getLinkedChests().isEmpty()) {
            player.sendMessage(ChatColor.RED + "This collector has no linked chests.");
            return;
        }

        // Create a GUI to display linked chests
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Linked Chests");

        // Add linked chests to the GUI
        int slot = 0;
        for (Location chestLocation : collector.getLinkedChests()) {
            ItemStack chestItem = createItem(
                    Material.CHEST,
                    ChatColor.GOLD + "Linked Chest",
                    List.of(
                            ChatColor.GRAY + "Location: " + formatLocation(chestLocation),
                            "",
                            ChatColor.YELLOW + "Click to unlink"
                    )
            );
            inventory.setItem(slot, chestItem);
            slot++;

            if (slot >= 27) {
                break; // Prevent overflow
            }
        }

        // Open the GUI
        player.openInventory(inventory);

        // TODO: Implement handling for clicks in this GUI
    }

    /**
     * Removes a player from the open GUIs cache.
     *
     * @param player The player
     */
    public void removePlayer(Player player) {
        openGUIs.remove(player);
    }

    /**
     * Checks if a player has an open GUI.
     *
     * @param player The player
     * @return true if the player has an open GUI, false otherwise
     */
    public boolean hasOpenGUI(Player player) {
        return openGUIs.containsKey(player);
    }
}
