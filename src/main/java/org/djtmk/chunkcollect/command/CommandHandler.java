package org.djtmk.chunkcollect.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.djtmk.chunkcollect.ChunkCollect;
import org.djtmk.chunkcollect.config.Config;
import org.djtmk.chunkcollect.data.CollectorData;
import org.djtmk.chunkcollect.gui.CollectorGUI;
import org.djtmk.chunkcollect.manager.CollectorManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles commands for the plugin.
 */
public class CommandHandler implements CommandExecutor, TabCompleter {
    private final ChunkCollect plugin;
    private final Config config;
    private final CollectorManager collectorManager;
    private final CollectorGUI collectorGUI;

    /**
     * Creates a new command handler.
     *
     * @param plugin The plugin instance
     * @param config The plugin configuration
     * @param collectorManager The collector manager
     * @param collectorGUI The collector GUI
     */
    public CommandHandler(ChunkCollect plugin, Config config, CollectorManager collectorManager, CollectorGUI collectorGUI) {
        this.plugin = plugin;
        this.config = config;
        this.collectorManager = collectorManager;
        this.collectorGUI = collectorGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreate(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "gui":
                return handleGUI(sender, args);
            case "list":
                return handleList(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "tp":
                return handleTeleport(sender, args);
            case "give":
                return handleGive(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * Handles the create command.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("chunkcollect.use")) {
            player.sendMessage(config.getMessage("no-permission"));
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || targetBlock.getType() != config.getCollectorBlockType()) {
            player.sendMessage(ChatColor.RED + "You must be looking at a " + config.getCollectorBlockType().name() + " to create a collector.");
            return true;
        }

        boolean created = collectorManager.createCollector(player, targetBlock);
        if (!created) {
            player.sendMessage(config.getMessage("max-collectors-reached"));
            return true;
        }

        player.sendMessage(config.getMessage("collector-placed"));
        return true;
    }

    /**
     * Handles the remove command.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("chunkcollect.use")) {
            player.sendMessage(config.getMessage("no-permission"));
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !collectorManager.isCollector(targetBlock)) {
            player.sendMessage(ChatColor.RED + "You must be looking at a collector to remove it.");
            return true;
        }

        boolean removed = collectorManager.removeCollector(targetBlock);
        if (!removed) {
            player.sendMessage(ChatColor.RED + "Failed to remove collector.");
            return true;
        }

        player.sendMessage(config.getMessage("collector-removed"));
        return true;
    }

    /**
     * Handles the GUI command.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleGUI(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("chunkcollect.use")) {
            player.sendMessage(config.getMessage("no-permission"));
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !collectorManager.isCollector(targetBlock)) {
            player.sendMessage(ChatColor.RED + "You must be looking at a collector to open its GUI.");
            return true;
        }

        CollectorData collector = collectorManager.getCollector(targetBlock);
        if (collector == null) {
            player.sendMessage(ChatColor.RED + "Failed to get collector data.");
            return true;
        }

        collectorGUI.openGUI(player, collector);
        return true;
    }

    /**
     * Handles the list command.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chunkcollect.admin")) {
            sender.sendMessage(config.getMessage("no-permission"));
            return true;
        }

        Map<String, CollectorData> collectors = collectorManager.getAllCollectors();

        if (collectors.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No collectors found.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Collectors (" + collectors.size() + "):");

        for (Map.Entry<String, CollectorData> entry : collectors.entrySet()) {
            String id = entry.getKey();
            CollectorData collector = entry.getValue();
            Location loc = collector.getBlockLocation();

            sender.sendMessage(ChatColor.AQUA + id + ChatColor.GRAY + " - " + 
                    ChatColor.WHITE + loc.getWorld().getName() + " " + 
                    loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }

        return true;
    }

    /**
     * Handles the reload command.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chunkcollect.admin")) {
            sender.sendMessage(config.getMessage("no-permission"));
            return true;
        }

        config.loadConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
        return true;
    }

    /**
     * Handles the teleport command.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("chunkcollect.admin")) {
            player.sendMessage(config.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /cc tp <id>");
            return true;
        }

        String id = args[1];
        Map<String, CollectorData> collectors = collectorManager.getAllCollectors();

        if (!collectors.containsKey(id)) {
            player.sendMessage(ChatColor.RED + "Collector not found: " + id);
            return true;
        }

        CollectorData collector = collectors.get(id);
        Location location = collector.getBlockLocation().clone().add(0.5, 1, 0.5);

        player.teleport(location);
        player.sendMessage(ChatColor.GREEN + "Teleported to collector: " + id);
        return true;
    }

    /**
     * Handles the give command.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chunkcollect.give")) {
            sender.sendMessage(config.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /cc give <player> <amount>");
            return true;
        }

        String playerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                sender.sendMessage(ChatColor.RED + "Amount must be a positive number.");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Amount must be a number.");
            return true;
        }

        // Give the player the collectors
        // Since we can't directly give collectors (they need to be placed),
        // we'll give the player the collector block items
        ItemStack collectorItem = new ItemStack(config.getCollectorBlockType(), amount);

        // Add the items to the player's inventory
        HashMap<Integer, ItemStack> notAdded = targetPlayer.getInventory().addItem(collectorItem);

        if (notAdded.isEmpty()) {
            sender.sendMessage(config.getMessage("collector-given", 
                    new String[]{"%amount%", "%player%"}, 
                    new String[]{String.valueOf(amount), targetPlayer.getName()}));
            targetPlayer.sendMessage(config.getMessage("collector-received", 
                    new String[]{"%amount%"}, 
                    new String[]{String.valueOf(amount)}));
        } else {
            int notAddedAmount = 0;
            for (ItemStack item : notAdded.values()) {
                notAddedAmount += item.getAmount();
            }
            int addedAmount = amount - notAddedAmount;

            sender.sendMessage(config.getMessage("collector-given", 
                    new String[]{"%amount%", "%player%"}, 
                    new String[]{String.valueOf(addedAmount), targetPlayer.getName()}));
            sender.sendMessage(ChatColor.RED + "Could not give " + notAddedAmount + " collector" + (notAddedAmount > 1 ? "s" : "") + " due to full inventory.");
            targetPlayer.sendMessage(config.getMessage("collector-received", 
                    new String[]{"%amount%"}, 
                    new String[]{String.valueOf(addedAmount)}));
        }

        return true;
    }

    /**
     * Sends help information to a command sender.
     *
     * @param sender The command sender
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "ChunkCollect+ Commands:");
        sender.sendMessage(ChatColor.AQUA + "/cc create" + ChatColor.GRAY + " - Place a collector");
        sender.sendMessage(ChatColor.AQUA + "/cc remove" + ChatColor.GRAY + " - Remove a collector");
        sender.sendMessage(ChatColor.AQUA + "/cc gui" + ChatColor.GRAY + " - Open the collector GUI");

        if (sender.hasPermission("chunkcollect.admin")) {
            sender.sendMessage(ChatColor.AQUA + "/cc list" + ChatColor.GRAY + " - List all collectors");
            sender.sendMessage(ChatColor.AQUA + "/cc reload" + ChatColor.GRAY + " - Reload plugin configuration");
            sender.sendMessage(ChatColor.AQUA + "/cc tp <id>" + ChatColor.GRAY + " - Teleport to a collector");
        }

        if (sender.hasPermission("chunkcollect.give")) {
            sender.sendMessage(ChatColor.AQUA + "/cc give <player> <amount>" + ChatColor.GRAY + " - Give collectors to a player");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("create", "remove", "gui"));

            if (sender.hasPermission("chunkcollect.admin")) {
                completions.addAll(Arrays.asList("list", "reload", "tp"));
            }

            if (sender.hasPermission("chunkcollect.give")) {
                completions.add("give");
            }

            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("tp") && sender.hasPermission("chunkcollect.admin")) {
                return collectorManager.getAllCollectors().keySet().stream()
                        .filter(s -> s.startsWith(args[1]))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("give") && sender.hasPermission("chunkcollect.give")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("chunkcollect.give")) {
            return Arrays.asList("1", "5", "10", "64");
        }

        return List.of();
    }
}
