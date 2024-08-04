package org.spectrum.chunkdeleter;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class ChunkDeleter extends JavaPlugin implements Listener {

    // Configuration file names
    private static final String OVERWORLD_CONFIG_FILE = "overworld.yml";
    private static final String NETHER_CONFIG_FILE = "the_nether.yml";
    private static final String END_CONFIG_FILE = "the_end.yml";

    private FileConfiguration overworldConfig;
    private FileConfiguration netherConfig;
    private FileConfiguration endConfig;

    @Override
    public void onEnable() {
        // Save default configurations if they don't exist
        saveResource(OVERWORLD_CONFIG_FILE, false);
        saveResource(NETHER_CONFIG_FILE, false);
        saveResource(END_CONFIG_FILE, false);

        loadConfigs();
        getServer().getPluginManager().registerEvents(this, this);

        // Log configuration details
        getLogger().info("ChunkDeleter plugin enabled!");
        logConfigDetails();
    }

    @Override
    public void onDisable() {
        saveConfigs();
    }

    private void loadConfigs() {
        overworldConfig = loadConfig(OVERWORLD_CONFIG_FILE);
        netherConfig = loadConfig(NETHER_CONFIG_FILE);
        endConfig = loadConfig(END_CONFIG_FILE);
    }

    private FileConfiguration loadConfig(String fileName) {
        File configFile = new File(getDataFolder(), fileName);
        if (!configFile.exists()) {
            saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(configFile);
    }

    private void saveConfigs() {
        saveConfig(overworldConfig, OVERWORLD_CONFIG_FILE);
        saveConfig(netherConfig, NETHER_CONFIG_FILE);
        saveConfig(endConfig, END_CONFIG_FILE);
    }

    private void saveConfig(FileConfiguration config, String fileName) {
        try {
            config.save(new File(getDataFolder(), fileName));
        } catch (IOException e) {
            getLogger().warning("Could not save " + fileName + " config file!");
        }
    }

    private void logConfigDetails() {
        getLogger().info("Overworld protect radius: " + overworldConfig.getInt("protect-radius"));
        getLogger().info("Nether protect radius: " + netherConfig.getInt("protect-radius"));
        getLogger().info("End protect radius: " + endConfig.getInt("protect-radius"));
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) {
            FileConfiguration config = getConfigForWorld(event.getWorld().getName());
            if (config == null) return;
            getLogger().info("Find chunk (" + event.getChunk().getX() + "," + event.getChunk().getZ() + ") in world " + event.getWorld().getName() + " as untouched.");

            int protectRadius = config.getInt("protect-radius");
            if (protectRadius == -1) {
                // 如果保护半径为 -1，不标记任何新区块
                return;
            }

            // 检查区块是否在保护半径内
            boolean isWithinProtectRadius = isWithinProtectRadius(event.getChunk().getX(), event.getChunk().getZ(), protectRadius);

            // 如果区块不在保护半径内，则标记为未触碰
            if (!isWithinProtectRadius) {
                markChunkAsUntouched(event.getChunk().getX(), event.getChunk().getZ(), config);
                getLogger().info("Mark chunk (" + event.getChunk().getX() + "," + event.getChunk().getZ() + ") in world " + event.getWorld().getName() + "as untouched.");
            }
        }
    }


    private boolean isWithinProtectRadius(int chunkX, int chunkZ, int radius) {
        return chunkX * chunkX + chunkZ * chunkZ <= radius * radius;
    }

    private void markChunkAsUntouched(int chunkX, int chunkZ, FileConfiguration config) {
        Set<String> untouchedChunks = new HashSet<>(config.getStringList("untouched-chunks"));
        String chunkKey = getChunkKey(chunkX, chunkZ);
        if (untouchedChunks.add(chunkKey)) {
            config.set("untouched-chunks", new ArrayList<>(untouchedChunks));
            getLogger().info("Marked chunk successfully!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            String chunkKey = getChunkKey(event.getClickedBlock().getChunk().getX(), event.getClickedBlock().getChunk().getZ());
            FileConfiguration config = getConfigForWorld(event.getPlayer().getWorld().getName());
            if (config == null) return;
            getLogger().info("Find chunk (" + event.getClickedBlock().getChunk().getX() + "," + event.getClickedBlock().getChunk().getZ() + ") in world " + event.getClickedBlock().getWorld().getName() + " as touched.");

            removeChunkFromUntouchedList(chunkKey, config);
        }
    }

    private void removeChunkFromUntouchedList(String chunkKey, FileConfiguration config) {
        Set<String> untouchedChunks = new HashSet<>(config.getStringList("untouched-chunks"));
        if (untouchedChunks.remove(chunkKey)) {
            config.set("untouched-chunks", new ArrayList<>(untouchedChunks));
            getLogger().info("Removed mark successfully!");
        }
    }

    private FileConfiguration getConfigForWorld(String worldName) {
        switch (worldName) {
            case "world":
                return overworldConfig;
            case "world_nether":
                return netherConfig;
            case "world_the_end":
                return endConfig;
            default:
                return null;
        }
    }

    private String getChunkKey(int chunkX, int chunkZ) {
        return chunkX + "," + chunkZ;
    }
}
