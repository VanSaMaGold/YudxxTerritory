package yudxx.minecraft.spigot.yudxxTerritory.gui;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotData;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotSettings;
import yudxx.minecraft.spigot.yudxxTerritory.listener.MenuListener;
import yudxx.minecraft.spigot.yudxxTerritory.manager.ConfigManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.PlotManager;
import yudxx.minecraft.spigot.yudxxTerritory.util.HexColorUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettingsMenu {

    private final PlotManager plotManager;
    private final ConfigManager configManager;

    private static final int[] SLOTS = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34};

    private static class SettingDef {
        final String key;
        final String configPath;
        final Material material;
        final String type;

        SettingDef(String key, String configPath, Material material, String type) {
            this.key = key;
            this.configPath = configPath;
            this.material = material;
            this.type = type;
        }
    }

    private static final SettingDef[] ALL_SETTINGS = {
        new SettingDef("time", "settings-menu.time-item", Material.CLOCK, "cycle"),
        new SettingDef("weather", "settings-menu.weather-item", Material.WATER_BUCKET, "cycle"),
        new SettingDef("pvp", "settings-menu.pvp-item", Material.DIAMOND_SWORD, "bool"),
        new SettingDef("monster-spawning", "settings-menu.monster-item", Material.ZOMBIE_HEAD, "bool"),
        new SettingDef("animal-spawning", "settings-menu.animal-item", Material.COW_SPAWN_EGG, "bool"),
        new SettingDef("fire-spread", "settings-menu.fire-item", Material.FIRE_CHARGE, "bool"),
        new SettingDef("explosions", "settings-menu.explosion-item", Material.TNT, "bool"),
        new SettingDef("enter-message", "settings-menu.enter-msg-item", Material.OAK_SIGN, "bool"),
        new SettingDef("leave-message", "settings-menu.leave-msg-item", Material.BARRIER, "bool"),
        new SettingDef("gamemode", "settings-menu.gamemode-item", Material.ENDER_CHEST, "cycle"),
        new SettingDef("item-drops", "settings-menu.item-drops-item", Material.DIAMOND, "bool"),
        new SettingDef("item-discard", "settings-menu.item-discard-item", Material.HOPPER, "bool"),
    };

    private List<SettingDef> activeSettings;

    public SettingsMenu(PlotManager plotManager, ConfigManager configManager) {
        this.plotManager = plotManager;
        this.configManager = configManager;
        buildActiveSettings();
    }

    private void buildActiveSettings() {
        activeSettings = new ArrayList<>();
        for (SettingDef def : ALL_SETTINGS) {
            if (configManager.isSettingEnabled(def.key)) {
                activeSettings.add(def);
            }
        }
    }

    private int getSlotForIndex(int index) {
        return SLOTS[index];
    }

    private int getIndexForSlot(int slot) {
        for (int i = 0; i < activeSettings.size(); i++) {
            if (SLOTS[i] == slot) return i;
        }
        return -1;
    }

    public void openSettings(Player player, PlotData plot) {
        buildActiveSettings();
        FileConfiguration gui = configManager.getGuiConfig();
        String title = MessageFormat.format(
            gui.getString("settings-menu.title", "&8Territory Settings - &b{0}, {1}"),
            plot.getPlotX(), plot.getPlotZ()
        );
        Inventory inv = Bukkit.createInventory(null, 54, HexColorUtil.color(title));
        PlotSettings settings = plot.getSettings();

        for (int i = 0; i < activeSettings.size(); i++) {
            SettingDef def = activeSettings.get(i);
            int slot = getSlotForIndex(i);
            String currentValue;
            String rawValue;
            switch (def.type) {
                case "cycle":
                    if (def.key.equals("time")) {
                        rawValue = settings.getString("time");
                        currentValue = getTimeDisplay(gui, rawValue);
                    } else if (def.key.equals("weather")) {
                        rawValue = settings.getString("weather");
                        currentValue = getWeatherDisplay(gui, rawValue);
                    } else {
                        rawValue = settings.getString("gamemode");
                        currentValue = getGamemodeDisplay(gui, rawValue);
                    }
                    break;
                default:
                    boolean bval = settings.getBoolean(def.key);
                    rawValue = bval ? "on" : "off";
                    currentValue = getBoolDisplay(gui, bval);
                    break;
            }
            inv.setItem(slot, createToggle(gui, def.material, def.configPath, currentValue, rawValue));
        }

        String backName = gui.getString("settings-menu.back-item.name", "&aBack to Main Menu");
        List<String> backLore = colorLore(gui.getStringList("settings-menu.back-item.lore"));
        inv.setItem(49, createItem(Material.ARROW, backName, backLore));

        player.openInventory(inv);
        MenuListener.registerMenu(player, "settings");
    }

    private String getTimeDisplay(FileConfiguration gui, String time) {
        String key = "display.time." + time;
        return gui.getString(key, time);
    }

    private String getWeatherDisplay(FileConfiguration gui, String weather) {
        String key = "display.weather." + weather;
        return gui.getString(key, weather);
    }

    private String getBoolDisplay(FileConfiguration gui, boolean val) {
        return val
            ? gui.getString("display.enabled", "&aEnabled")
            : gui.getString("display.disabled", "&cDisabled");
    }

    private String getGamemodeDisplay(FileConfiguration gui, String gm) {
        String key = "display.gamemode." + gm;
        return gui.getString(key, gm);
    }

    public void handleSettingClick(Player player, PlotData plot, int slot) {
        int index = getIndexForSlot(slot);
        if (index < 0) return;
        SettingDef def = activeSettings.get(index);
        PlotSettings settings = plot.getSettings();

        switch (def.key) {
            case "time": cycleTime(settings); break;
            case "weather": cycleWeather(settings); break;
            case "gamemode": cycleGamemode(settings); break;
            default: toggleBool(settings, def.key); break;
        }

        plotManager.savePlotSettings(plot);
        applySettingsToAllPlayersInPlot(plot);
        openSettings(player, plot);
    }

    private void applySettingsToAllPlayersInPlot(PlotData plot) {
        PlotSettings settings = plot.getSettings();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals(plot.getWorldName())) continue;
            PlotData playerPlot = plotManager.getPlotAtLocation(p.getLocation());
            if (playerPlot == null || !playerPlot.getPlotKey().equals(plot.getPlotKey())) continue;
            applySinglePlayerSettings(p, settings);
        }
    }

    public static void applySinglePlayerSettings(Player player, PlotSettings settings) {
        String time = settings.getString("time");
        if (!time.equals("default")) {
            long timeTicks;
            switch (time) {
                case "day": timeTicks = 1000; break;
                case "sunset": timeTicks = 12000; break;
                case "night": timeTicks = 13000; break;
                case "sunrise": timeTicks = 23000; break;
                default: timeTicks = 1000;
            }
            player.setPlayerTime(timeTicks, false);
        } else {
            player.resetPlayerTime();
        }
        String weather = settings.getString("weather");
        if (!weather.equals("default")) {
            switch (weather) {
                case "clear":
                    player.setPlayerWeather(org.bukkit.WeatherType.CLEAR);
                    break;
                case "rain":
                    player.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL);
                    break;
                case "thunder":
                    player.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL);
                    player.getWorld().setThundering(true);
                    break;
            }
        } else {
            player.resetPlayerWeather();
        }
        String gm = settings.getString("gamemode");
        if (!player.hasPermission("yudxxterritory.admin.bypass")) {
            if (!gm.equals("default")) {
                try {
                    GameMode gameMode = GameMode.valueOf(gm.toUpperCase());
                    player.setGameMode(gameMode);
                } catch (IllegalArgumentException ignored) {
                }
            } else {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
    }

    private void cycleTime(PlotSettings settings) {
        String current = settings.getString("time");
        switch (current) {
            case "day": settings.set("time", "sunset"); break;
            case "sunset": settings.set("time", "night"); break;
            case "night": settings.set("time", "sunrise"); break;
            case "sunrise": settings.set("time", "default"); break;
            default: settings.set("time", "day"); break;
        }
    }

    private void cycleWeather(PlotSettings settings) {
        String current = settings.getString("weather");
        switch (current) {
            case "clear": settings.set("weather", "rain"); break;
            case "rain": settings.set("weather", "thunder"); break;
            case "thunder": settings.set("weather", "default"); break;
            default: settings.set("weather", "clear"); break;
        }
    }

    private void cycleGamemode(PlotSettings settings) {
        String current = settings.getString("gamemode");
        switch (current) {
            case "survival": settings.set("gamemode", "creative"); break;
            case "creative": settings.set("gamemode", "adventure"); break;
            case "adventure": settings.set("gamemode", "spectator"); break;
            case "spectator": settings.set("gamemode", "default"); break;
            default: settings.set("gamemode", "survival"); break;
        }
    }

    private void toggleBool(PlotSettings settings, String key) {
        settings.set(key, !settings.getBoolean(key));
    }

    private ItemStack createToggle(FileConfiguration gui, Material material, String configPath, String currentValue, String rawValue) {
        String name = gui.getString(configPath + ".name", "&eSetting");
        List<String> rawLore = gui.getStringList(configPath + ".lore");
        List<String> lore = new ArrayList<>();
        for (String line : rawLore) {
            line = MessageFormat.format(line, currentValue);
            lore.add(HexColorUtil.color(line));
        }
        return createItem(material, name, lore);
    }

    private List<String> colorLore(List<String> raw) {
        List<String> result = new ArrayList<>();
        for (String line : raw) {
            result.add(HexColorUtil.color(line));
        }
        return result;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(HexColorUtil.color(name));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getConfigSettingsTitle() {
        FileConfiguration gui = configManager.getGuiConfig();
        return gui.getString("settings-menu.title", "&8Territory Settings");
    }
}