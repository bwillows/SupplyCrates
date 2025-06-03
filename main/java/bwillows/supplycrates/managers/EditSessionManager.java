package bwillows.supplycrates.managers;

import bwillows.supplycrates.model.EnvoyType;
import bwillows.supplycrates.storage.DataHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;


public class EditSessionManager {

    private final DataHandler dataHandler;

    // Tracks which player is editing which envoy
    private final Map<UUID, String> editingPlayers = new HashMap<>();

    // Stores original blocks per player to restore them properly
    private final Map<UUID, Map<Location, Material>> replacedBlocks = new HashMap<>();

    public EditSessionManager(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    public boolean isEditing(Player player) {
        return editingPlayers.containsKey(player.getUniqueId());
    }

    public String getEditingEnvoyId(Player player) {
        return editingPlayers.get(player.getUniqueId());
    }

    public void toggleEditMode(Player player, String envoyId) {
        UUID uuid = player.getUniqueId();

        // If already editing, stop
        if (editingPlayers.containsKey(uuid)) {
            stopEditing(player);
            return;
        }

        // Start editing
        EnvoyType envoy = dataHandler.getEnvoy(envoyId);
        if (envoy == null) {
            player.sendMessage("§cUnknown envoy type: §e" + envoyId);
            return;
        }

        editingPlayers.put(uuid, envoyId);

        // Track and replace all crate locations with emerald blocks
        Map<Location, Material> playerReplacements = new HashMap<>();
        for (Location loc : envoy.getCrateLocations()) {
            Block block = loc.getBlock();
            if (block.getType() != Material.EMERALD_BLOCK) {
                playerReplacements.put(loc, block.getType());
                block.setType(Material.EMERALD_BLOCK);
            }
        }
        replacedBlocks.put(uuid, playerReplacements);

        // Give the player 1 emerald block
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.EMERALD_BLOCK, 1));

        player.sendMessage("§aEditing started for envoy §e" + envoyId + "§a. Break emerald blocks to remove locations, place them to add.");
    }

    public void stopEditing(Player player) {
        UUID uuid = player.getUniqueId();
        String envoyId = editingPlayers.remove(uuid);
        if (envoyId == null) return;

        EnvoyType envoy = dataHandler.getEnvoy(envoyId);
        if (envoy != null) {
            for (Location loc : envoy.getCrateLocations()) {
                Block block = loc.getBlock();
                if (block.getType() == Material.EMERALD_BLOCK) {
                    block.setType(Material.AIR);
                }
            }
        }

        replacedBlocks.remove(uuid); // cleanup tracking map

        // Remove the editing tool from the player's inventory
        ItemStack editTool = new ItemStack(Material.EMERALD_BLOCK);
        player.getInventory().removeItem(editTool);

        player.sendMessage("§cEditing ended for envoy §e" + envoyId + "§c.");
    }

    public boolean isTypeBeingEdited(String typeId) {
        for (String editingType : editingPlayers.values()) {
            if (editingType.equalsIgnoreCase(typeId)) {
                return true;
            }
        }
        return false;
    }

    public List<Player> getEditorsOfType(String typeId) {
        List<Player> players = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : editingPlayers.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(typeId)) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    public void handleBlockPlace(Player player, Location loc) {
        if (!isEditing(player)) return;

        if (loc.getBlock().getType() != Material.EMERALD_BLOCK) return;

        String envoyId = getEditingEnvoyId(player);
        dataHandler.addLocationToEnvoy(envoyId, loc);
        player.sendMessage("§aAdded new crate location.");
    }

    public void handleBlockBreak(Player player, Location loc) {
        if (!isEditing(player)) return;

        String envoyId = getEditingEnvoyId(player);
        dataHandler.removeLocationFromEnvoy(envoyId, loc);
        player.sendMessage("§cRemoved crate location.");
    }

    public void endAllSessions() {
        for (UUID uuid : new HashSet<>(editingPlayers.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                stopEditing(player);
            }
        }
    }
}