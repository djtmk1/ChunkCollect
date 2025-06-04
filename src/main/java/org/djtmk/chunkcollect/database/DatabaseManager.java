package org.djtmk.chunkcollect.database;

import org.djtmk.chunkcollect.data.CollectorData;

import java.util.Map;

/**
 * Interface for database operations.
 */
public interface DatabaseManager {
    
    /**
     * Initializes the database.
     * 
     * @return true if initialization was successful, false otherwise
     */
    boolean initialize();
    
    /**
     * Loads all collectors from the database.
     * 
     * @return a map of collector IDs to collector data
     */
    Map<String, CollectorData> loadCollectors();
    
    /**
     * Saves all collectors to the database.
     * 
     * @param collectors a map of collector IDs to collector data
     * @return true if saving was successful, false otherwise
     */
    boolean saveCollectors(Map<String, CollectorData> collectors);
    
    /**
     * Saves a single collector to the database.
     * 
     * @param id the collector ID
     * @param collector the collector data
     * @return true if saving was successful, false otherwise
     */
    boolean saveCollector(String id, CollectorData collector);
    
    /**
     * Deletes a collector from the database.
     * 
     * @param id the collector ID
     * @return true if deletion was successful, false otherwise
     */
    boolean deleteCollector(String id);
    
    /**
     * Closes the database connection.
     */
    void close();
}