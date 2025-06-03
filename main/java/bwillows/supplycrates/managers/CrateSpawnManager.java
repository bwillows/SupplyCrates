package bwillows.supplycrates.managers;

import bwillows.supplycrates.model.EnvoyType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CrateSpawnManager {

    private final Plugin plugin;

    // Active crates: block location -> envoy type
    private final Map<Location, EnvoyType> activeCrates = new HashMap<>();

    public CrateSpawnManager(Plugin plugin) {
        this.plugin = plugin;
    }

    private final Map<String, EnvoyType> envoyTypes = new HashMap<>();


    public void registerEnvoyType(EnvoyType type) {
        envoyTypes.put(type.getId().toLowerCase(), type);
    }

    public EnvoyType getEnvoyType(String name) {
        return envoyTypes.get(name.toLowerCase());
    }

    public Set<String> getAllEnvoyTypeNames() {
        return envoyTypes.keySet();
    }

    /**
     * Checks if an envoy type has any active crates.
     */
    public boolean isEnvoyActive(EnvoyType type) {
        for (Map.Entry<Location, EnvoyType> entry : activeCrates.entrySet()) {
            if (entry.getValue().getId().equalsIgnoreCase(type.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Spawns all crates for a given envoy type
     */
    public void startEnvoy(EnvoyType type) {

        for (Location loc : type.getCrateLocations()) {
            spawnCrate(loc, type);
        }

    }

    /**
     * Removes all active crate blocks from the world for a given envoy
     */
    public void stopEnvoy(EnvoyType type) {
        for (Location loc : type.getCrateLocations()) {
            Block block = loc.getBlock();
            if (block.getType() == type.getBlockType()) {
                block.setType(Material.AIR);
            }
            activeCrates.remove(loc);
        }

        // Remove all holograms
        if (type.getHolograms() != null) {
            for (List<ArmorStand> stands : type.getHolograms().values()) {
                for (ArmorStand stand : stands) {
                    if (!stand.isDead()) {
                        stand.remove();
                    }
                }
            }
            type.getHolograms().clear();
        }

    }

    /**
     * Spawns a single falling crate from a location and lands it
     */
    public void spawnCrate(Location targetLoc, EnvoyType type) {
        // Set the crate block
        targetLoc.getBlock().setType(type.getBlockType());
        activeCrates.put(targetLoc, type);

        Location crateLoc = targetLoc.getBlock().getLocation();
        List<String> lines = type.getHologramLines();

        // Defensive check
        if (lines == null || lines.isEmpty()) {
            Bukkit.getLogger().warning("[SupplyCrates] No hologram lines found for envoy " + type.getId());
            return;
        }

        // Delay hologram spawn slightly to ensure world/chunk is ready
        new BukkitRunnable() {
            @Override
            public void run() {
                double baseY = crateLoc.getY() + 0.2; // bottom line sits slightly above crate
                double spacing = 0.25;

                for (int i = lines.size() - 1; i >= 0; i--) {
                    String line = ChatColor.translateAlternateColorCodes('&', lines.get(i));
                    double yOffset = baseY + ((lines.size() - 1 - i) * spacing);

                    Location holoLoc = new Location(
                            crateLoc.getWorld(),
                            crateLoc.getX() + 0.5,
                            yOffset,
                            crateLoc.getZ() + 0.5
                    );

                    Entity entity = crateLoc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
                    if (!(entity instanceof ArmorStand)) {
                        Bukkit.getLogger().warning("[SupplyCrates] Failed to spawn armor stand at " + holoLoc);
                        continue;
                    }

                    ArmorStand stand = (ArmorStand) entity;
                    stand.setCustomName(line);
                    stand.setCustomNameVisible(true);
                    stand.setVisible(false);
                    stand.setGravity(false);
                    stand.setSmall(true);

                    type.addHologram(crateLoc, stand);
                }
            }
        }.runTaskLater(plugin, 2L); // Slight delay to avoid spawn failures

    }

    /**
     * Checks if a block is a known active crate
     */
    public boolean isCrate(Location loc) {
        return activeCrates.containsKey(loc);
    }

    /**
     * Gets the envoy type associated with a crate block
     */
    public EnvoyType getEnvoyAt(Location loc) {
        return activeCrates.get(loc);
    }

    /**
     * Removes the crate from memory (e.g., after being opened)
     */


    public void removeCrate(Location loc) {
        EnvoyType type = activeCrates.remove(loc);
        if (type == null) return;

        boolean anyRemainingForType = activeCrates.values().stream()
                .anyMatch(t -> t.equals(type));

        if (!anyRemainingForType) {
            stopEnvoy(type);

        }
    }

    public Map<Location, EnvoyType> getActiveCrates() {
        return activeCrates;
    }

    public void registerCrate(Location loc, EnvoyType type) {
        activeCrates.put(loc, type);
    }
}
