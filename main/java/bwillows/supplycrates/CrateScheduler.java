package bwillows.supplycrates;

import bwillows.supplycrates.managers.CrateSpawnManager;
import bwillows.supplycrates.managers.EditSessionManager;
import bwillows.supplycrates.model.EnvoyType;
import bwillows.supplycrates.storage.DataHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class CrateScheduler {

    private final SupplyCrates plugin;
    private final CrateSpawnManager crateSpawnManager;
    private final DataHandler dataHandler;

    // Seconds between drops for each crate type
    private final Map<String, Integer> crateIntervals = new HashMap<>();

    // Next drop time in milliseconds for each crate type
    private final Map<String, Long> nextDropTimes = new HashMap<>();

    public CrateScheduler(SupplyCrates plugin, CrateSpawnManager crateSpawnManager, DataHandler dataHandler) {
        this.plugin = plugin;
        this.crateSpawnManager = crateSpawnManager;
        this.dataHandler = dataHandler;
    }

    public void loadCrateIntervals() {
        crateIntervals.clear();
        nextDropTimes.clear();

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("crate-intervals");

        if (section != null) {
            for (String type : section.getKeys(false)) {
                int seconds = section.getInt(type, -1);
                if (seconds > 0) {
                    crateIntervals.put(type.toLowerCase(), seconds);
                    nextDropTimes.put(type.toLowerCase(), System.currentTimeMillis() + (seconds * 1000L));
                }
            }
        }
    }

    public void startAutoScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (String typeString : crateIntervals.keySet()) {
                    EnvoyType type = dataHandler.getEnvoy(typeString);
                    if (type == null) {
                        Bukkit.getLogger().warning("[SupplyCrates] Skipping drop: envoy type '" + typeString + "' not found.");
                        continue;
                    }

                    long next = nextDropTimes.getOrDefault(typeString, now);

                    if (now >= next) {
                        // Check if it's being edited
                        EditSessionManager editManager = ((SupplyCrates) plugin).getEditManager();
                        if (editManager.isTypeBeingEdited(typeString)) {
                            Bukkit.getLogger().info("[SupplyCrates] Skipping '" + typeString + "' drop — it is being edited.");

                            String msg = plugin.getConfig().getString("messages.drop_skipped_while_editing",
                                            "&eThe &6{envoy} &edrop was skipped because someone is editing it.")
                                    .replace("{envoy}", typeString);

                            for (Player editor : plugin.getEditManager().getEditorsOfType(typeString)) {
                                editor.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                            }

                            // Still reschedule for the next interval
                            int intervalSeconds = crateIntervals.get(typeString);
                            nextDropTimes.put(typeString, now + intervalSeconds * 1000L);
                            continue;
                        }

                        // Not being edited – start the drop
                        crateSpawnManager.startEnvoy(type);

                        int intervalSeconds = crateIntervals.get(typeString);
                        nextDropTimes.put(typeString, now + intervalSeconds * 1000L);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Runs every second
    }

    public Map<String, Long> getNextDropTimes() {
        return nextDropTimes;
    }

    public long getTimeUntilNextDrop(String type) {
        type = type.toLowerCase();
        if (!nextDropTimes.containsKey(type)) return -1;
        long now = System.currentTimeMillis();
        long next = nextDropTimes.get(type);
        return Math.max(0, next - now);
    }

    public boolean isCrateScheduled(String type) {
        return crateIntervals.containsKey(type.toLowerCase());
    }
}
