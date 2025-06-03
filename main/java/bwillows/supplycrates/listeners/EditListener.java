package bwillows.supplycrates.listeners;

import bwillows.supplycrates.managers.EditSessionManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class EditListener implements Listener {

    private final EditSessionManager editSessionManager;

    public EditListener(EditSessionManager editSessionManager) {
        this.editSessionManager = editSessionManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!editSessionManager.isEditing(player)) return;
        if (block.getType() != Material.EMERALD_BLOCK) return;

        editSessionManager.handleBlockPlace(player, block.getLocation());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!editSessionManager.isEditing(player)) return;
        if (block.getType() != Material.EMERALD_BLOCK) return;

        event.setCancelled(true); // prevent drop
        block.setType(Material.AIR);
        editSessionManager.handleBlockBreak(player, block.getLocation());
    }
}
