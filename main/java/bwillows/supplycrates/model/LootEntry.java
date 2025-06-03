package bwillows.supplycrates.model;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LootEntry {

    public enum EntryType {
        ITEM,
        COMMAND
    }

    private final EntryType type;
    private final double chance;

    // One of these will be used depending on type
    private final ItemStack item;
    private final List<String> commands;

    public LootEntry(ItemStack item, double chance) {
        this.type = EntryType.ITEM;
        this.item = item;
        this.commands = null;
        this.chance = chance;
    }

    public LootEntry(List<String> commands, double chance) {
        this.type = EntryType.COMMAND;
        this.item = null;
        this.commands = commands;
        this.chance = chance;
    }


    public EntryType getType() {
        return type;
    }

    public double getChance() {
        return chance;
    }

    public ItemStack getItem() {
        return item;
    }

    public List<String> getCommands() {
        return commands;
    }
}
