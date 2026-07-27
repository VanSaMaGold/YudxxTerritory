package yudxx.minecraft.spigot.yudxxTerritory.manager;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {

    private final JavaPlugin plugin;

    private FileConfiguration config;
    private File configFile;

    private FileConfiguration worldsConfig;
    private File worldsFile;

    private FileConfiguration guiConfig;
    private File guiFile;

    private int defaultPlotSize;
    private int defaultRoadWidth;
    private int defaultPlotHeight;
    private String defaultWorld;
    private int maxPlotsPerPlayer;
    private double autoClaimCost;
    private boolean economyEnabled;
    private int undoTimeoutSeconds;
    private int voidTeleportY;
    private int noBuildY;
    private Map<String, WorldSettings> worldSettings;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.worldSettings = new HashMap<>();
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        worldsFile = new File(plugin.getDataFolder(), "worlds.yml");
        if (!worldsFile.exists()) {
            plugin.saveResource("worlds.yml", false);
        }
        worldsConfig = YamlConfiguration.loadConfiguration(worldsFile);

        guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        loadSettings();
        loadWorldSettings();
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        worldsConfig = YamlConfiguration.loadConfiguration(worldsFile);
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        worldSettings.clear();
        loadSettings();
        loadWorldSettings();
    }

    private void loadSettings() {
        defaultPlotSize = config.getInt("plot.default-size", 64);
        defaultRoadWidth = config.getInt("plot.default-road-width", 3);
        defaultPlotHeight = config.getInt("plot.default-height", 64);
        defaultWorld = config.getString("plot.default-world", "plotworld");
        maxPlotsPerPlayer = config.getInt("plot.max-plots-non-admin", 5);
        autoClaimCost = config.getDouble("economy.auto-claim-cost", 0.0);
        economyEnabled = config.getBoolean("economy.enabled", false);
        undoTimeoutSeconds = config.getInt("settings.undo-timeout-seconds", 30);
        voidTeleportY = config.getInt("protection.void-teleport-y", 10);
        noBuildY = config.getInt("protection.no-build-y", 15);
    }

    private void loadWorldSettings() {
        ConfigurationSection worldsSection = worldsConfig.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                ConfigurationSection ws = worldsSection.getConfigurationSection(worldName);
                if (ws != null) {
                    Material borderMat = parseMaterial(ws.getString("border-material", "OAK_SLAB"));
                    Material roadMat = parseMaterial(ws.getString("road-material", "STONE"));
                    Material fillMat = parseMaterial(ws.getString("fill-material", "GRASS_BLOCK"));
                    WorldSettings settings = new WorldSettings(
                        ws.getInt("plot-size", defaultPlotSize),
                        ws.getInt("road-width", defaultRoadWidth),
                        ws.getInt("plot-height", defaultPlotHeight),
                        ws.getInt("max-range", 100),
                        ws.getBoolean("enabled", true),
                        borderMat,
                        roadMat,
                        ws.getInt("road-radius", 0),
                        fillMat
                    );
                    worldSettings.put(worldName, settings);
                }
            }
        }
    }

    public WorldSettings getWorldSettings(String worldName) {
        if (worldSettings.containsKey(worldName)) {
            return worldSettings.get(worldName);
        }
        WorldSettings defaultSettings = new WorldSettings(
            defaultPlotSize, defaultRoadWidth, defaultPlotHeight, 100, true,
            Material.OAK_SLAB, Material.STONE, 0, Material.GRASS_BLOCK
        );
        worldSettings.put(worldName, defaultSettings);
        return defaultSettings;
    }

    private Material parseMaterial(String name) {
        if (name == null) return Material.OAK_SLAB;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.OAK_SLAB;
        }
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    public int getDefaultPlotSize() {
        return defaultPlotSize;
    }

    public int getDefaultRoadWidth() {
        return defaultRoadWidth;
    }

    public int getDefaultPlotHeight() {
        return defaultPlotHeight;
    }

    public String getDefaultWorld() {
        return defaultWorld;
    }

    public int getMaxPlotsPerPlayer() {
        return maxPlotsPerPlayer;
    }

    public double getAutoClaimCost() {
        return autoClaimCost;
    }

    public boolean isEconomyEnabled() {
        return economyEnabled;
    }

    public boolean isSettingEnabled(String key) {
        return config.getBoolean("settings.enabled." + key, true);
    }

    public int getUndoTimeoutSeconds() {
        return undoTimeoutSeconds;
    }

    public int getVoidTeleportY() {
        return voidTeleportY;
    }

    public int getNoBuildY() {
        return noBuildY;
    }

    public List<String> getPlotWorlds() {
        List<String> worlds = new ArrayList<>();
        ConfigurationSection worldsSection = worldsConfig.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String key : worldsSection.getKeys(false)) {
                ConfigurationSection ws = worldsSection.getConfigurationSection(key);
                if (ws != null && ws.getBoolean("enabled", true)) {
                    worlds.add(key);
                }
            }
        }
        if (worlds.isEmpty()) {
            worlds.add(defaultWorld);
        }
        return worlds;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveWorldsConfig() {
        try {
            worldsConfig.save(worldsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setWorldBorderMaterial(String worldName, Material material) {
        worldsConfig.set("worlds." + worldName + ".border-material", material.name());
        saveWorldsConfig();
        WorldSettings old = worldSettings.get(worldName);
        if (old != null) {
            WorldSettings updated = new WorldSettings(
                old.getPlotSize(), old.getRoadWidth(), old.getPlotHeight(),
                old.getMaxRange(), old.isEnabled(), material,
                old.getRoadMaterial(), old.getRoadRadius(), old.getFillMaterial()
            );
            worldSettings.put(worldName, updated);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getWorldsConfig() {
        return worldsConfig;
    }

    public static class WorldSettings {
        private final int plotSize;
        private final int roadWidth;
        private final int plotHeight;
        private final int maxRange;
        private final boolean enabled;
        private final Material borderMaterial;
        private final Material roadMaterial;
        private final int roadRadius;
        private final Material fillMaterial;

        public WorldSettings(int plotSize, int roadWidth, int plotHeight, int maxRange, boolean enabled,
                             Material borderMaterial, Material roadMaterial, int roadRadius, Material fillMaterial) {
            this.plotSize = plotSize;
            this.roadWidth = roadWidth;
            this.plotHeight = plotHeight;
            this.maxRange = maxRange;
            this.enabled = enabled;
            this.borderMaterial = borderMaterial;
            this.roadMaterial = roadMaterial;
            this.roadRadius = roadRadius;
            this.fillMaterial = fillMaterial;
        }

        public int getPlotSize() {
            return plotSize;
        }

        public int getRoadWidth() {
            return roadWidth;
        }

        public int getPlotHeight() {
            return plotHeight;
        }

        public int getMaxRange() {
            return maxRange;
        }

        public boolean isInfinite() {
            return maxRange <= 0;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getTotalSize() {
            return plotSize + roadWidth;
        }

        public Material getBorderMaterial() {
            return borderMaterial;
        }

        public Material getRoadMaterial() {
            return roadMaterial;
        }

        public int getRoadRadius() {
            return roadRadius <= 0 ? roadWidth : roadRadius;
        }

        public Material getFillMaterial() {
            return fillMaterial;
        }
    }
}