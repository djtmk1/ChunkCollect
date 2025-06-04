package org.djtmk.chunkcollect.database;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.djtmk.chunkcollect.ChunkCollect;
import org.djtmk.chunkcollect.config.Config;
import org.djtmk.chunkcollect.data.CollectorData;

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
 * MySQL implementation of the DatabaseManager interface.
 */
public class MySQLManager implements DatabaseManager {
    private final ChunkCollect plugin;
    private final Config config;
    private Connection connection;
    private final String tablePrefix;

    /**
     * Creates a new MySQL database manager.
     *
     * @param plugin The plugin instance
     * @param config The plugin configuration
     */
    public MySQLManager(ChunkCollect plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.tablePrefix = config.getMysqlTablePrefix();
    }

    @Override
    public boolean initialize() {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

            // Open a connection to the database
            String url = "jdbc:mysql://" + config.getMysqlHost() + ":" + config.getMysqlPort() + "/" + config.getMysqlDatabase() +
                    "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8";
            connection = DriverManager.getConnection(url, config.getMysqlUsername(), config.getMysqlPassword());

            // Create tables if they don't exist
            createTables();

            return true;
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize MySQL database", e);
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
                    "CREATE TABLE IF NOT EXISTS " + tablePrefix + "collectors (" +
                            "id VARCHAR(36) PRIMARY KEY, " +
                            "owner VARCHAR(36) NOT NULL, " +
                            "world VARCHAR(64) NOT NULL, " +
                            "x INT NOT NULL, " +
                            "y INT NOT NULL, " +
                            "z INT NOT NULL, " +
                            "chunk_x INT NOT NULL, " +
                            "chunk_z INT NOT NULL, " +
                            "speed_level INT NOT NULL, " +
                            "range_level INT NOT NULL, " +
                            "max_linked_chests INT NOT NULL" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // Create filters table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS " + tablePrefix + "filters (" +
                            "collector_id VARCHAR(36) NOT NULL, " +
                            "material VARCHAR(64) NOT NULL, " +
                            "PRIMARY KEY (collector_id, material), " +
                            "FOREIGN KEY (collector_id) REFERENCES " + tablePrefix + "collectors(id) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // Create linked chests table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS " + tablePrefix + "linked_chests (" +
                            "collector_id VARCHAR(36) NOT NULL, " +
                            "world VARCHAR(64) NOT NULL, " +
                            "x INT NOT NULL, " +
                            "y INT NOT NULL, " +
                            "z INT NOT NULL, " +
                            "PRIMARY KEY (collector_id, world, x, y, z), " +
                            "FOREIGN KEY (collector_id) REFERENCES " + tablePrefix + "collectors(id) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        }
    }

    @Override
    public Map<String, CollectorData> loadCollectors() {
        Map<String, CollectorData> collectors = new HashMap<>();

        try {
            // Load collectors
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM " + tablePrefix + "collectors"
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
            plugin.getLogger().log(Level.SEVERE, "Failed to load collectors from MySQL database", e);
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
                "SELECT material FROM " + tablePrefix + "filters WHERE collector_id = ?"
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
                "SELECT world, x, y, z FROM " + tablePrefix + "linked_chests WHERE collector_id = ?"
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
                statement.execute("DELETE FROM " + tablePrefix + "collectors");
                statement.execute("DELETE FROM " + tablePrefix + "filters");
                statement.execute("DELETE FROM " + tablePrefix + "linked_chests");
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
            plugin.getLogger().log(Level.SEVERE, "Failed to save collectors to MySQL database", e);

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
                    "INSERT INTO " + tablePrefix + "collectors " +
                            "(id, owner, world, x, y, z, chunk_x, chunk_z, speed_level, range_level, max_linked_chests) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "owner = VALUES(owner), " +
                            "world = VALUES(world), " +
                            "x = VALUES(x), " +
                            "y = VALUES(y), " +
                            "z = VALUES(z), " +
                            "chunk_x = VALUES(chunk_x), " +
                            "chunk_z = VALUES(chunk_z), " +
                            "speed_level = VALUES(speed_level), " +
                            "range_level = VALUES(range_level), " +
                            "max_linked_chests = VALUES(max_linked_chests)"
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
            plugin.getLogger().log(Level.SEVERE, "Failed to save collector " + id + " to MySQL database", e);
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
                "DELETE FROM " + tablePrefix + "filters WHERE collector_id = ?"
        )) {
            statement.setString(1, collectorId);
            statement.executeUpdate();
        }

        // Insert new filters
        if (!filters.isEmpty()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO " + tablePrefix + "filters (collector_id, material) VALUES (?, ?)"
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
                "DELETE FROM " + tablePrefix + "linked_chests WHERE collector_id = ?"
        )) {
            statement.setString(1, collectorId);
            statement.executeUpdate();
        }

        // Insert new linked chests
        if (!linkedChests.isEmpty()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO " + tablePrefix + "linked_chests (collector_id, world, x, y, z) VALUES (?, ?, ?, ?, ?)"
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
                    "DELETE FROM " + tablePrefix + "collectors WHERE id = ?"
            )) {
                statement.setString(1, id);
                int rowsAffected = statement.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete collector " + id + " from MySQL database", e);
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
            plugin.getLogger().log(Level.SEVERE, "Failed to close MySQL database connection", e);
        }
    }
}