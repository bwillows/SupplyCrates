package bwillows.supplycrates.storage;

import bwillows.supplycrates.SupplyCrates;
import bwillows.supplycrates.model.EnvoyType;
import bwillows.supplycrates.model.LootEntry;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DataHandler {

    private final SupplyCrates plugin;
    private final Map<String, EnvoyType> envoyTypes = new HashMap<>();
    private final File envoysFolder;
    private final boolean debug;

    public DataHandler(SupplyCrates plugin) {
        this.plugin = plugin;
        this.envoysFolder = new File(plugin.getDataFolder(), "envoys");
        if (!envoysFolder.exists()) {
            envoysFolder.mkdirs();
        }

        this.debug = plugin.getConfig().getBoolean("debug", false);
    }

    public void loadEnvoys() {
        envoyTypes.clear();

        File[] files = envoysFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            if (debug) plugin.getLogger().warning("No envoy files found.");
            return;
        }

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = file.getName().replace(".yml", "");

                if (debug) plugin.getLogger().info("Loading envoy: " + id);

                EnvoyType envoy = new EnvoyType(id);

                List<String> hologramLines = config.getStringList("hologram");
                envoy.setHologramLines(hologramLines);

                Bukkit.getLogger().info("Loaded hologram lines for envoy " + id + ": " + hologramLines);

                // Load block type
                String blockStr = config.getString("blockType", plugin.getConfig().getString("default_block_type", "CHEST"));
                Material blockType = Material.getMaterial(blockStr);
                if (blockType == null) blockType = Material.CHEST;
                envoy.setBlockType(blockType);


                // Load locations
                List<Map<?, ?>> locationMaps = config.getMapList("locations");
                for (Map<?, ?> locMap : locationMaps) {
                    try {
                        String world = (String) locMap.get("world");
                        int x = ((Number) locMap.get("x")).intValue();
                        int y = ((Number) locMap.get("y")).intValue();
                        int z = ((Number) locMap.get("z")).intValue();

                        World w = Bukkit.getWorld(world);
                        if (w == null) {
                            if (debug) plugin.getLogger().warning("World not found: " + world);
                            continue;
                        }

                        Location loc = new Location(w, x, y, z);
                        envoy.addLocation(loc);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid location in " + file.getName());
                        if (debug) e.printStackTrace();
                    }
                }

                // Load loot
                List<Map<?, ?>> lootList = config.getMapList("loot");
                for (Map<?, ?> loot : lootList) {
                    try {
                        String type = ((String) loot.get("type")).toLowerCase();
                        double chance = ((Number) loot.get("chance")).doubleValue();

                        if (type.equals("item")) {
                            String materialName = (String) loot.get("item");
                            Material material = Material.getMaterial(materialName);
                            if (material == null) continue;

                            int amount = 1;
                            Object amountObj = loot.get("amount");
                            if (amountObj instanceof Number) {
                                amount = ((Number) amountObj).intValue();
                            }

                            ItemStack item = new ItemStack(material, amount);
                            envoy.addLoot(new LootEntry(item, chance));

                        } else if (type.equals("command")) {
                            Object raw = loot.get("commands");
                            List<String> commands;

                            if (raw instanceof List) {
                                commands = ((List<?>) raw).stream()
                                        .filter(Objects::nonNull)
                                        .map(Object::toString)
                                        .collect(Collectors.toList());
                            } else {
                                // fallback for old single-command format
                                commands = Collections.singletonList((String) loot.get("command"));
                            }

                            envoy.addLoot(new LootEntry(commands, chance));
                        }

                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid loot entry in " + file.getName());
                        if (debug) e.printStackTrace();
                    }
                }

                plugin.getCrateSpawnManager().registerEnvoyType(envoy);

                envoyTypes.put(id.toLowerCase(), envoy);
                if (debug) plugin.getLogger().info("  Loaded " + envoy.getCrateLocations().size() + " locations and " +
                        envoy.getLootTable().size() + " loot entries for '" + id + "'.");

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load envoy from file: " + file.getName());
                if (debug) e.printStackTrace();
            }
        }
    }

    public EnvoyType getEnvoy(String id) {
        return envoyTypes.get(id.toLowerCase());
    }

    public Collection<EnvoyType> getAllEnvoys() {
        return envoyTypes.values();
    }

    public void saveEnvoy(EnvoyType envoy) {
        File file = new File(envoysFolder, envoy.getId() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("hologram", envoy.getHologramText());
        config.set("blockType", envoy.getBlockType().name());

        List<Map<String, Object>> locList = new ArrayList<>();
        for (Location loc : envoy.getCrateLocations()) {
            Map<String, Object> locMap = new HashMap<>();
            locMap.put("world", loc.getWorld().getName());
            locMap.put("x", loc.getBlockX());
            locMap.put("y", loc.getBlockY());
            locMap.put("z", loc.getBlockZ());
            locList.add(locMap);
        }
        config.set("locations", locList);

        List<Map<String, Object>> lootList = new ArrayList<>();
        for (LootEntry entry : envoy.getLootTable()) {
            Map<String, Object> lootMap = new HashMap<>();
            lootMap.put("chance", entry.getChance());

            switch (entry.getType()) {
                case ITEM:
                    lootMap.put("type", "item");
                    lootMap.put("item", entry.getItem().getType().name());
                    lootMap.put("amount", entry.getItem().getAmount());
                    break;
                case COMMAND:
                    lootMap.put("type", "command");
                    lootMap.put("commands", entry.getCommands());
                    break;
            }

            lootList.add(lootMap);
        }

        config.set("loot", lootList);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save envoy file: " + file.getName());
            e.printStackTrace();
        }
    }

    public void addLocationToEnvoy(String envoyId, Location loc) {
        EnvoyType envoy = getEnvoy(envoyId);
        if (envoy == null) return;
        envoy.addLocation(loc);
        saveEnvoy(envoy);
    }

    public void removeLocationFromEnvoy(String envoyId, Location loc) {
        EnvoyType envoy = getEnvoy(envoyId);
        if (envoy == null) return;
        envoy.removeLocation(loc);
        saveEnvoy(envoy);
    }
}