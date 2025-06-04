package org.djtmk.chunkcollect;

import org.bukkit.plugin.java.JavaPlugin;
import org.djtmk.chunkcollect.command.CommandHandler;
import org.djtmk.chunkcollect.config.Config;
import org.djtmk.chunkcollect.gui.CollectorGUI;
import org.djtmk.chunkcollect.listener.CollectorListener;
import org.djtmk.chunkcollect.listener.GUIListener;
import org.djtmk.chunkcollect.manager.CollectorManager;
import org.djtmk.chunkcollect.task.CollectorTask;

/**
 * Main plugin class for ChunkCollect+.
 */
public final class ChunkCollect extends JavaPlugin {
    private Config config;
    private CollectorManager collectorManager;
    private CollectorGUI collectorGUI;
    private CollectorTask collectorTask;

    @Override
    public void onEnable() {
        // Initialize configuration
        config = new Config(this);

        // Initialize managers
        collectorManager = new CollectorManager(this, config);

        // Initialize GUI
        collectorGUI = new CollectorGUI(this, config, collectorManager);

        // Register commands
        CommandHandler commandHandler = new CommandHandler(this, config, collectorManager, collectorGUI);
        getCommand("chunkcollect").setExecutor(commandHandler);
        getCommand("chunkcollect").setTabCompleter(commandHandler);

        // Register listeners
        getServer().getPluginManager().registerEvents(new CollectorListener(this, collectorManager, config), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this, collectorGUI), this);

        // Start collector task
        collectorTask = new CollectorTask(this, collectorManager, config);
        collectorTask.runTaskTimer(this, 20L, 1L); // Run every tick, starting after 1 second

        getLogger().info("ChunkCollect+ has been enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel tasks
        if (collectorTask != null) {
            collectorTask.cancel();
        }

        // Save collectors and close database
        if (collectorManager != null) {
            collectorManager.saveCollectors();
            collectorManager.closeDatabase();
        }

        getLogger().info("ChunkCollect+ has been disabled!");
    }
}
