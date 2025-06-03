package bwillows.supplycrates;

import bwillows.supplycrates.commands.SupplyCratesCommand;
import bwillows.supplycrates.listeners.CrateListener;
import bwillows.supplycrates.listeners.EditListener;
import bwillows.supplycrates.managers.CrateSpawnManager;
import bwillows.supplycrates.managers.EditSessionManager;
import bwillows.supplycrates.model.EnvoyType;
import bwillows.supplycrates.storage.DataHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SupplyCrates extends JavaPlugin {
    private DataHandler dataHandler;
    private CrateSpawnManager crateSpawnManager;
    private EditSessionManager editManager;
    private CrateScheduler crateScheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize core components
        this.dataHandler = new DataHandler(this);
        this.crateSpawnManager = new CrateSpawnManager(this);
        this.crateScheduler = new CrateScheduler(this, crateSpawnManager, this.dataHandler);

        File envoysFolder = new File(getDataFolder(), "envoys");
        if (!envoysFolder.exists()) {
            envoysFolder.mkdirs();
        }

        // Save example.yml into the envoys folder on first run
        File exampleFile = new File(envoysFolder, "example.yml");
        if (!exampleFile.exists()) {
            saveResource("envoys/example.yml", false);
        }

        // Load all envoy types from /envoys/
        dataHandler.loadEnvoys();

        // Register command
        editManager = new EditSessionManager(dataHandler);
        getCommand("supplycrates").setExecutor(new SupplyCratesCommand(this, dataHandler, crateSpawnManager, editManager));
        getServer().getPluginManager().registerEvents(new EditListener(editManager), this);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(
                new CrateListener(crateSpawnManager, this), this
        );

        Bukkit.getPluginManager().registerEvents(new CrateListener(crateSpawnManager, this), this);

        crateScheduler.loadCrateIntervals();
        crateScheduler.startAutoScheduler();

        getLogger().info("SupplyCrates enabled.");
    }

    @Override
    public void onDisable() {
        if (crateSpawnManager != null) {
            for (EnvoyType type : dataHandler.getAllEnvoys()) {
                crateSpawnManager.stopEnvoy(type); // This clears blocks + holograms
            }
        }
        editManager.endAllSessions();
    }

    public CrateScheduler getCrateScheduler() {
        return crateScheduler;
    }
    public CrateSpawnManager getCrateSpawnManager() { return crateSpawnManager; }
    public EditSessionManager getEditManager() { return editManager; }
}
