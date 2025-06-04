package org.djtmk.chunkcollect.database;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.djtmk.chunkcollect.ChunkCollect;
import org.djtmk.chunkcollect.data.CollectorData;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQLite implementation of the DatabaseManager interface.
 */
public class SQLiteManager implements DatabaseManager {
    private final ChunkCollect plugin;
    private final String dbFile;
    private Connection connection;

    /**
     * Creates a new SQLite database manager.
     *
     * @param plugin The plugin instance
     */
    public SQLiteManager(ChunkCollect plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "collectors.db").getAbsolutePath();
    }

    @Override
    public boolean initialize() {
        try {
            // Ensure the plugin data folder exists
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Open a connection to the database
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);

            // Create tables if they don't exist
            createTables();

            return true;
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite database", e);
            return false;
        }
    }

    /**
     * Creates the necessary tables in the database.
     *
     * @throws SQLException if a database error occurs
     */
    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Create collectors table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS collectors (" +
                            "id TEXT PRIMARY KEY, " +
                            "owner TEXT NOT NULL, " +
                            "world TEXT NOT NULL, " +
                            "x INTEGER NOT NULL, " +
                            "y INTEGER NOT NULL, " +
                            "z INTEGER NOT NULL, " +
                            "chunk_x INTEGER NOT NULL, " +
                            "chunk_z INTEGER NOT NULL, " +
                            "speed_level INTEGER NOT NULL, " +
                            "range_level INTEGER NOT NULL, " +
                            "max_linked_chests INTEGER NOT NULL" +
                            ")"
            );

            // Create filters table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS filters (" +
                            "collector_id TEXT NOT NULL, " +
                            "material TEXT NOT NULL, " +
                            "PRIMARY KEY (collector_id, material), " +
                            "FOREIGN KEY (collector_id) REFERENCES collectors(id) ON DELETE CASCADE" +
                            ")"
            );

            // Create linked chests table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS linked_chests (" +
                            "collector_id TEXT NOT NULL, " +
                            "world TEXT NOT NULL, " +
                            "x INTEGER NOT NULL, " +
                            "y INTEGER NOT NULL, " +
                            "z INTEGER NOT NULL, " +
                            "PRIMARY KEY (collector_id, world, x, y, z), " +
                            "FOREIGN KEY (collector_id) REFERENCES collectors(id) ON DELETE CASCADE" +
                            ")"
            );
        }
    }

    @Override
    public Map<String, CollectorData> loadCollectors() {
        Map<String, CollectorData> collectors = new HashMap<>();

        try {
            // Load collectors
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM collectors"
            )) {
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String id = resultSet.getString("id");
                    UUID owner = UUID.fromString(resultSet.getString("owner"));
                    String worldName = resultSet.getString("world");
                    int x = resultSet.getInt("x");
                    int y = resultSet.getInt("y");
                    int z = resultSet.getInt("z");
                    int chunkX = resultSet.getInt("chunk_x");
                    int chunkZ = resultSet.getInt("chunk_z");
                    int speedLevel = resultSet.getInt("speed_level");
                    int rangeLevel = resultSet.getInt("range_level");
                    int maxLinkedChests = resultSet.getInt("max_linked_chests");

                    // Get the world and chunk
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        plugin.getLogger().warning("Failed to load collector " + id + ": world " + worldName + " not found");
                        continue;
                    }

                    Location blockLocation = new Location(world, x, y, z);
                    Chunk chunk = world.getChunkAt(chunkX, chunkZ);

                    // Create the collector data
                    CollectorData collector = new CollectorData(owner, blockLocation, chunk);
                    collector.setSpeedLevel(speedLevel);
                    collector.setRangeLevel(rangeLevel);
                    collector.setMaxLinkedChests(maxLinkedChests);

                    // Load filters
                    List<Material> filters = loadFilters(id);
                    collector.setFilterList(filters);

                    // Load linked chests
                    List<Location> linkedChests = loadLinkedChests(id);
                    collector.setLinkedChests(linkedChests);

                    collectors.put(id, collector);
                }
            }

            return collectors;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load collectors from SQLite database", e);
            return new HashMap<>();
        }
    }

    /**
     * Loads filters for a collector.
     *
     * @param collectorId The collector ID
     * @return A list of materials
     * @throws SQLException if a database error occurs
     */
    private List<Material> loadFilters(String collectorId) throws SQLException {
        List<Material> filters = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT material FROM filters WHERE collector_id = ?"
        )) {
            statement.setString(1, collectorId);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String materialName = resultSet.getString("material");
                try {
                    Material material = Material.valueOf(materialName);
                    filters.add(material);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in database: " + materialName);
                }
            }
        }

        return filters;
    }

    /**
     * Loads linked chests for a collector.
     *
     * @param collectorId The collector ID
     * @return A list of locations
     * @throws SQLException if a database error occurs
     */
    private List<Location> loadLinkedChests(String collectorId) throws SQLException {
        List<Location> linkedChests = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT world, x, y, z FROM linked_chests WHERE collector_id = ?"
        )) {
            statement.setString(1, collectorId);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String worldName = resultSet.getString("world");
                int x = resultSet.getInt("x");
                int y = resultSet.getInt("y");
                int z = resultSet.getInt("z");

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Failed to load linked chest: world " + worldName + " not found");
                    continue;
                }

                Location location = new Location(world, x, y, z);
                linkedChests.add(location);
            }
        }

        return linkedChests;
    }

    @Override
    public boolean saveCollectors(Map<String, CollectorData> collectors) {
        try {
            // Begin transaction
            connection.setAutoCommit(false);

            // Clear existing data
            try (Statement statement = connection.createStatement()) {
                statement.execute("DELETE FROM collectors");
                statement.execute("DELETE FROM filters");
                statement.execute("DELETE FROM linked_chests");
            }

            // Save collectors
            for (Map.Entry<String, CollectorData> entry : collectors.entrySet()) {
                String id = entry.getKey();
                CollectorData collector = entry.getValue();

                saveCollector(id, collector);
            }

            // Commit transaction
            connection.commit();
            connection.setAutoCommit(true);

            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save collectors to SQLite database", e);

            // Rollback transaction
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to rollback transaction", ex);
            }

            return false;
        }
    }

    @Override
    public boolean saveCollector(String id, CollectorData collector) {
        try {
            // Save collector
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT OR REPLACE INTO collectors " +
                            "(id, owner, world, x, y, z, chunk_x, chunk_z, speed_level, range_level, max_linked_chests) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )) {
                statement.setString(1, id);
                statement.setString(2, collector.getOwner().toString());
                statement.setString(3, collector.getBlockLocation().getWorld().getName());
                statement.setInt(4, collector.getBlockLocation().getBlockX());
                statement.setInt(5, collector.getBlockLocation().getBlockY());
                statement.setInt(6, collector.getBlockLocation().getBlockZ());
                statement.setInt(7, collector.getChunk().getX());
                statement.setInt(8, collector.getChunk().getZ());
                statement.setInt(9, collector.getSpeedLevel());
                statement.setInt(10, collector.getRangeLevel());
                statement.setInt(11, collector.getMaxLinkedChests());

                statement.executeUpdate();
            }

            // Save filters
            saveFilters(id, collector.getFilterList());

            // Save linked chests
            saveLinkedChests(id, collector.getLinkedChests());

            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save collector " + id + " to SQLite database", e);
            return false;
        }
    }

    /**
     * Saves filters for a collector.
     *
     * @param collectorId The collector ID
     * @param filters The list of materials
     * @throws SQLException if a database error occurs
     */
    private void saveFilters(String collectorId, List<Material> filters) throws SQLException {
        // Delete existing filters
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM filters WHERE collector_id = ?"
        )) {
            statement.setString(1, collectorId);
            statement.executeUpdate();
        }

        // Insert new filters
        if (!filters.isEmpty()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO filters (collector_id, material) VALUES (?, ?)"
            )) {
                for (Material material : filters) {
                    statement.setString(1, collectorId);
                    statement.setString(2, material.name());
                    statement.executeUpdate();
                }
            }
        }
    }

    /**
     * Saves linked chests for a collector.
     *
     * @param collectorId The collector ID
     * @param linkedChests The list of locations
     * @throws SQLException if a database error occurs
     */
    private void saveLinkedChests(String collectorId, List<Location> linkedChests) throws SQLException {
        // Delete existing linked chests
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM linked_chests WHERE collector_id = ?"
        )) {
            statement.setString(1, collectorId);
            statement.executeUpdate();
        }

        // Insert new linked chests
        if (!linkedChests.isEmpty()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO linked_chests (collector_id, world, x, y, z) VALUES (?, ?, ?, ?, ?)"
            )) {
                for (Location location : linkedChests) {
                    statement.setString(1, collectorId);
                    statement.setString(2, location.getWorld().getName());
                    statement.setInt(3, location.getBlockX());
                    statement.setInt(4, location.getBlockY());
                    statement.setInt(5, location.getBlockZ());
                    statement.executeUpdate();
                }
            }
        }
    }

    @Override
    public boolean deleteCollector(String id) {
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM collectors WHERE id = ?"
            )) {
                statement.setString(1, id);
                int rowsAffected = statement.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete collector " + id + " from SQLite database", e);
            return false;
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close SQLite database connection", e);
        }
    }
}