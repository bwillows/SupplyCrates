package bwillows.supplycrates.listeners;

import bwillows.supplycrates.managers.CrateSpawnManager;
import bwillows.supplycrates.model.EnvoyType;
import bwillows.supplycrates.model.LootEntry;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;

import java.util.Random;

public class CrateListener implements Listener {

    private final CrateSpawnManager crateManager;
    private final Plugin plugin;
    private final Random random = new Random();

    private void spawnFirework(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        Firework firework = (Firework) world.spawn(loc.add(0.5, 1, 0.5), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.YELLOW)
                .withFade(Color.ORANGE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build());

        meta.setPower(0); // Short fuse — irrelevant here since we explode it instantly
        firework.setFireworkMeta(meta);

    }

    public CrateListener(CrateSpawnManager crateManager, Plugin plugin) {
        this.crateManager = crateManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onCrateInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Location loc = clickedBlock.getLocation();

        EnvoyType envoy = crateManager.getEnvoyAt(loc);
        if (envoy == null) return;

        event.setCancelled(true);

        // Loot the crate
        for (LootEntry loot : envoy.getLootTable()) {
            if (Math.random() <= loot.getChance()) {
                switch (loot.getType()) {
                    case ITEM:
                        player.getInventory().addItem(loot.getItem());
                        break;
                    case COMMAND:
                        for (String command : loot.getCommands()) {
                            Bukkit.dispatchCommand(
                                    Bukkit.getConsoleSender(),
                                    command.replace("{player}", player.getName())
                            );
                        }
                        break;
                }
            }
        }

        // Remove the crate block and visual
        clickedBlock.setType(Material.AIR);
        crateManager.removeCrate(loc);
        envoy.removeHologram(loc);

        if (plugin.getConfig().getBoolean("fireworks_on_crate_claim", true)) {
            spawnFirework(loc);
        }

        // Send personal message to player
        String personalMsg = plugin.getConfig().getString("messages.opened_crate", "&aYou looted a supply crate!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', personalMsg));

        // Count how many crates are still active for this envoy type
        long remaining = crateManager.getActiveCrates().values().stream()
                .filter(t -> t.equals(envoy))
                .count();

        // Broadcast crate claim message
        String broadcast = plugin.getConfig().getString("messages.crate_claimed");
        if (broadcast != null && !broadcast.isEmpty()) {
            String formatted = broadcast
                    .replace("{player}", player.getName())
                    .replace("{envoy}", envoy.getId())
                    .replace("{remaining}", String.valueOf(remaining));
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', formatted));
        }

        // If this was the last crate for this envoy, end the envoy
        if (remaining == 0) {
            crateManager.stopEnvoy(envoy);

            String endMsg = plugin.getConfig().getString("messages.envoy_ended_all_claimed",
                            "&6All &e{envoy} &6crates have been claimed. The drop has ended.")
                    .replace("{envoy}", envoy.getId());
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', endMsg));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if this block is an active crate
        if (crateManager.isCrate(block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot break a supply crate!");
        }
    }
}

