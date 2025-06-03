package bwillows.supplycrates.model;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EnvoyType {


    private List<String> hologramLines = new ArrayList<>();
    private final Map<Location, List<ArmorStand>> activeHolograms = new HashMap<>();

    public void addHologram(Location crateLoc, ArmorStand stand) {
        Location key = crateLoc.getBlock().getLocation(); // Ensure clean block location
        activeHolograms.computeIfAbsent(key, l -> new ArrayList<>()).add(stand);
    }

    public void removeHologram(Location crateLoc) {
        Location key = crateLoc.getBlock().getLocation();
        List<ArmorStand> stands = activeHolograms.remove(key);
        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (!stand.isDead()) {
                    stand.remove();
                }
            }
        }
    }

    public Map<Location, List<ArmorStand>> getHolograms() {
        return activeHolograms;
    }

    public void setHologramLines(List<String> lines) {
        this.hologramLines = lines != null ? lines : new ArrayList<>();
    }

    public List<String> getHologramLines() {
        return hologramLines;
    }

    private final String id;
    private final List<Location> crateLocations;
    private final List<LootEntry> lootTable;
    private Material blockType = Material.CHEST; // default crate block

    public EnvoyType(String id) {
        this.id = id;
        this.crateLocations = new ArrayList<>();
        this.lootTable = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public List<Location> getCrateLocations() {
        return crateLocations;
    }

    public void addLocation(Location loc) {
        crateLocations.add(loc);
    }

    public void removeLocation(Location loc) {
        crateLocations.remove(loc);
    }

    public List<LootEntry> getLootTable() {
        return lootTable;
    }

    public void addLoot(LootEntry entry) {
        lootTable.add(entry);
    }

    public List<String> getHologramText() {
        return hologramLines;
    }


    public Material getBlockType() {
        return blockType;
    }

    public void setBlockType(Material blockType) {
        this.blockType = blockType;
    }

}
