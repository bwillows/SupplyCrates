package bwillows.supplycrates.commands;

import bwillows.supplycrates.CrateScheduler;
import bwillows.supplycrates.SupplyCrates;
import bwillows.supplycrates.managers.CrateSpawnManager;
import bwillows.supplycrates.managers.EditSessionManager;
import bwillows.supplycrates.model.EnvoyType;
import bwillows.supplycrates.storage.DataHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class SupplyCratesCommand implements CommandExecutor {

    private final DataHandler dataHandler;
    private final CrateSpawnManager crateManager;
    private final EditSessionManager editManager;
    private final JavaPlugin plugin;

    public SupplyCratesCommand(JavaPlugin plugin, DataHandler dataHandler, CrateSpawnManager crateManager, EditSessionManager editManager) {
        this.plugin = plugin;
        this.dataHandler = dataHandler;
        this.crateManager = crateManager;
        this.editManager = editManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.prefix", "&7[&eSupplyCrates&7] "));

        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage(prefix + ChatColor.RED + "Console must specify a subcommand.");
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("supplycrates.admin")) {
                String noPerm = plugin.getConfig().getString("messages.no_permission", "&cYou don't have permission to do that.");
                player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', noPerm));
                return true;
            }
        }

        if (args.length == 0) {
            sender.sendMessage(prefix + ChatColor.RED + "Usage: /supplycrates <start|stop|edit|list|reload|time> [type]");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            sender.sendMessage(prefix + ChatColor.GREEN + "Available envoy types:");
            for (EnvoyType type : dataHandler.getAllEnvoys()) {
                sender.sendMessage(ChatColor.YELLOW + " - " + type.getId());
            }
            return true;
        }

        if (sub.equals("reload")) {
            plugin.reloadConfig();
            dataHandler.loadEnvoys();
            sender.sendMessage(prefix + ChatColor.GREEN + "SupplyCrates config and envoys reloaded.");
            return true;
        }

        if (sub.equals("time")) {
            CrateScheduler scheduler = ((SupplyCrates) plugin).getCrateScheduler();

            if (args.length == 1) {
                sender.sendMessage(prefix + ChatColor.YELLOW + "Supply crate drop times:");
                for (Map.Entry<String, Long> entry : scheduler.getNextDropTimes().entrySet()) {
                    long timeLeft = scheduler.getTimeUntilNextDrop(entry.getKey());
                    sender.sendMessage(" - " + ChatColor.GREEN + entry.getKey() + ": " + ChatColor.AQUA + formatTime(timeLeft));
                }
                return true;
            }

            String typeId = args[1].toLowerCase();
            if (!scheduler.isCrateScheduled(typeId)) {
                sender.sendMessage(prefix + ChatColor.RED + "That crate type is not scheduled.");
                return true;
            }

            long timeLeft = scheduler.getTimeUntilNextDrop(typeId);
            sender.sendMessage(prefix + ChatColor.YELLOW + "Next drop for " + ChatColor.GREEN + typeId
                    + ChatColor.YELLOW + " is in " + ChatColor.AQUA + formatTime(timeLeft));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + ChatColor.RED + "Only players can use this subcommand.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(prefix + ChatColor.RED + "Usage: /supplycrates " + sub + " <type>");
            return true;
        }

        String typeId = args[1].toLowerCase();
        EnvoyType envoy = dataHandler.getEnvoy(typeId);

        if (envoy == null) {
            player.sendMessage(prefix + ChatColor.RED + "Envoy type '" + typeId + "' not found.");
            return true;
        }

        if (sub.equals("start")) {
            if (crateManager.isEnvoyActive(envoy)) {
                crateManager.stopEnvoy(envoy);
            }
            EnvoyType envoyType = crateManager.getEnvoyType(typeId);
            String startedMsg = plugin.getConfig()
                    .getString("messages.envoy_started", "&eThe &6{envoy}&e envoy has started!")
                    .replace("{envoy}", envoy.getId());
            Bukkit.broadcastMessage(prefix + ChatColor.translateAlternateColorCodes('&', startedMsg));
            crateManager.startEnvoy(envoy);
            return true;
        }

        if (sub.equals("stop")) {
            if (!crateManager.isEnvoyActive(envoy)) {
                player.sendMessage(prefix + ChatColor.RED + "That envoy is not currently active.");
                return true;
            }
            crateManager.stopEnvoy(envoy);
            String stoppedMsg = plugin.getConfig()
                    .getString("messages.envoy_stopped", "&cThe &6{envoy}&c envoy has ended.")
                    .replace("{envoy}", envoy.getId());
            Bukkit.broadcastMessage(prefix + ChatColor.translateAlternateColorCodes('&', stoppedMsg));
            return true;
        }

        if (sub.equals("edit")) {
            if (crateManager.isEnvoyActive(envoy)) {
                player.sendMessage(prefix + ChatColor.RED + "You can't edit an envoy while it's active.");
                return true;
            }

            if (editManager.isEditing(player)) {
                editManager.stopEditing(player);
                player.sendMessage(prefix + ChatColor.YELLOW + "Exited edit mode.");
            } else {
                editManager.toggleEditMode(player, typeId);
                player.sendMessage(prefix + ChatColor.GREEN + "You are now editing envoy: " + envoy.getId());
            }
            return true;
        }

        player.sendMessage(prefix + ChatColor.RED + "Unknown subcommand.");
        return true;
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return minutes + "m " + remainingSeconds + "s";
    }
}