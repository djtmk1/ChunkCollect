package org.djtmk.chunkcollect.config;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.djtmk.chunkcollect.ChunkCollect;

/**
 * Handles the plugin configuration.
 */
public class Config {
    private final ChunkCollect plugin;

    // Configuration values
    private int maxCollectorsPerChunk;
    private int defaultCollectionInterval;
    private int defaultStorageRange;
    private Material collectorBlockType;
    private boolean filtersEnabled;
    private boolean economyEnabled;

    // Database settings
    private String databaseType;
    private String sqliteFile;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private String mysqlTablePrefix;

    // Chest linking settings
    private boolean autoLinkingEnabled;
    private int maxAutoLinkedChests;
    private int maxManualLinkedChests;
    private boolean autoLinkOnPlacement;

    // No longer needed as we're using SQLite only

    // Messages
    private String messagePrefix;
    private String messageCollectorPlaced;
    private String messageCollectorRemoved;
    private String messageNoPermission;
    private String messageMaxCollectorsReached;
    private String messageChestLinked;
    private String messageChestUnlinked;
    private String messageMaxLinkedChestsReached;
    private String messageChestLinkingMode;
    private String messageChestLinkingCancelled;
    private String messageChestAlreadyLinked;
    private String messageCollectorGiven;
    private String messageCollectorReceived;
    private String messageCollectorNotAdded;

    /**
     * Creates a new configuration handler.
     *
     * @param plugin The plugin instance
     */
    public Config(ChunkCollect plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    // The underlying configuration
    private FileConfiguration config;

    /**
     * Loads or reloads the configuration from disk.
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Load basic settings
        maxCollectorsPerChunk = config.getInt("max-collectors-per-chunk", 1);
        defaultCollectionInterval = config.getInt("default-collection-interval", 20);
        defaultStorageRange = config.getInt("default-storage-range", 5);

        // Parse collector block type
        String blockTypeStr = config.getString("collector-block-type", "HOPPER");
        try {
            collectorBlockType = Material.valueOf(blockTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid collector-block-type in config: " + blockTypeStr + ". Using HOPPER as default.");
            collectorBlockType = Material.HOPPER;
        }

        // Load feature flags
        filtersEnabled = config.getBoolean("filters-enabled", true);
        economyEnabled = config.getBoolean("economy-enabled", false);

        // Load chest linking settings
        autoLinkingEnabled = config.getBoolean("chest-linking.auto-linking-enabled", true);
        maxAutoLinkedChests = config.getInt("chest-linking.max-auto-linked-chests", 1);
        maxManualLinkedChests = config.getInt("chest-linking.max-manual-linked-chests", 3);
        autoLinkOnPlacement = config.getBoolean("chest-linking.auto-link-on-placement", true);

        // Load database settings
        databaseType = config.getString("database.type", "SQLITE").toUpperCase();
        sqliteFile = config.getString("database.sqlite.file", "collectors.db");
        mysqlHost = config.getString("database.mysql.host", "localhost");
        mysqlPort = config.getInt("database.mysql.port", 3306);
        mysqlDatabase = config.getString("database.mysql.database", "chunkcollect");
        mysqlUsername = config.getString("database.mysql.username", "root");
        mysqlPassword = config.getString("database.mysql.password", "password");
        mysqlTablePrefix = config.getString("database.mysql.table-prefix", "cc_");

        // Load messages
        messagePrefix = config.getString("messages.prefix", "&7[&bChunkCollect+&7] ");
        messageCollectorPlaced = config.getString("messages.collector-placed", "&aCollector placed successfully!");
        messageCollectorRemoved = config.getString("messages.collector-removed", "&cCollector removed.");
        messageNoPermission = config.getString("messages.no-permission", "&cYou don't have permission to do that.");
        messageMaxCollectorsReached = config.getString("messages.max-collectors-reached", "&cYou've reached the maximum number of collectors in this chunk.");
        messageChestLinked = config.getString("messages.chest-linked", "&aChest linked successfully!");
        messageChestUnlinked = config.getString("messages.chest-unlinked", "&cChest unlinked.");
        messageMaxLinkedChestsReached = config.getString("messages.max-linked-chests-reached", "&cYou've reached the maximum number of linked chests.");
        messageChestLinkingMode = config.getString("messages.chest-linking-mode", "&eClick a chest to link it to your collector.");
        messageChestLinkingCancelled = config.getString("messages.chest-linking-cancelled", "&cChest linking cancelled.");
        messageChestAlreadyLinked = config.getString("messages.chest-already-linked", "&cThis chest is already linked to a collector.");
        messageCollectorGiven = config.getString("messages.collector-given", "&aGave %amount% collector(s) to %player%.");
        messageCollectorReceived = config.getString("messages.collector-received", "&aYou received %amount% collector(s).");
        messageCollectorNotAdded = config.getString("messages.collector-not-added", "&cCould not give %amount% collector(s) due to full inventory.");
    }

    // Getters
    public int getMaxCollectorsPerChunk() {
        return maxCollectorsPerChunk;
    }

    public int getDefaultCollectionInterval() {
        return defaultCollectionInterval;
    }

    public int getDefaultStorageRange() {
        return defaultStorageRange;
    }

    public Material getCollectorBlockType() {
        return collectorBlockType;
    }

    public boolean isFiltersEnabled() {
        return filtersEnabled;
    }

    public boolean isEconomyEnabled() {
        return economyEnabled;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getSqliteFile() {
        return sqliteFile;
    }

    public String getMysqlHost() {
        return mysqlHost;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public String getMysqlTablePrefix() {
        return mysqlTablePrefix;
    }

    public boolean isAutoLinkingEnabled() {
        return autoLinkingEnabled;
    }

    public int getMaxAutoLinkedChests() {
        return maxAutoLinkedChests;
    }

    public int getMaxManualLinkedChests() {
        return maxManualLinkedChests;
    }

    public boolean isAutoLinkOnPlacement() {
        return autoLinkOnPlacement;
    }

    /**
     * Gets the underlying configuration.
     *
     * @return The configuration
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Gets a message from the configuration.
     *
     * @param key The message key
     * @return The formatted message
     */
    public String getMessage(String key) {
        String message;
        switch (key) {
            case "collector-placed":
                message = messageCollectorPlaced;
                break;
            case "collector-removed":
                message = messageCollectorRemoved;
                break;
            case "no-permission":
                message = messageNoPermission;
                break;
            case "max-collectors-reached":
                message = messageMaxCollectorsReached;
                break;
            case "chest-linked":
                message = messageChestLinked;
                break;
            case "chest-unlinked":
                message = messageChestUnlinked;
                break;
            case "max-linked-chests-reached":
                message = messageMaxLinkedChestsReached;
                break;
            case "chest-linking-mode":
                message = messageChestLinkingMode;
                break;
            case "chest-linking-cancelled":
                message = messageChestLinkingCancelled;
                break;
            case "chest-already-linked":
                message = messageChestAlreadyLinked;
                break;
            case "collector-given":
                message = messageCollectorGiven;
                break;
            case "collector-received":
                message = messageCollectorReceived;
                break;
            case "collector-not-added":
                message = messageCollectorNotAdded;
                break;
            default:
                message = "&cUnknown message: " + key;
        }

        return ChatColor.translateAlternateColorCodes('&', messagePrefix + message);
    }

    /**
     * Gets a message from the configuration with placeholders.
     *
     * @param key The message key
     * @param placeholders The placeholders to replace
     * @param values The values to replace the placeholders with
     * @return The formatted message
     */
    public String getMessage(String key, String[] placeholders, String[] values) {
        String message = getMessage(key);

        for (int i = 0; i < placeholders.length && i < values.length; i++) {
            message = message.replace(placeholders[i], values[i]);
        }

        return message;
    }
}
