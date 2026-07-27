package yudxx.minecraft.spigot.yudxxTerritory;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import yudxx.minecraft.spigot.yudxxTerritory.command.TerritoryAdminCommand;
import yudxx.minecraft.spigot.yudxxTerritory.command.TerritoryCommand;
import yudxx.minecraft.spigot.yudxxTerritory.data.DatabaseManager;
import yudxx.minecraft.spigot.yudxxTerritory.gui.MainMenu;
import yudxx.minecraft.spigot.yudxxTerritory.gui.PlotMenu;
import yudxx.minecraft.spigot.yudxxTerritory.gui.SettingsMenu;
import yudxx.minecraft.spigot.yudxxTerritory.listener.MenuListener;
import yudxx.minecraft.spigot.yudxxTerritory.listener.PlayerListener;
import yudxx.minecraft.spigot.yudxxTerritory.listener.PlotProtectListener;
import yudxx.minecraft.spigot.yudxxTerritory.manager.ClearManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.ConfigManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.MessageManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.PlotManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.SoundManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.WorldManager;

import java.util.Objects;

public final class YudxxTerritory extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private PlotManager plotManager;
    private WorldManager worldManager;
    private SoundManager soundManager;
    private ClearManager clearManager;
    private SettingsMenu settingsMenu;
    private PlotMenu plotMenu;
    private MainMenu mainMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        messageManager = new MessageManager(this);
        messageManager.loadMessages();

        soundManager = new SoundManager(this);
        soundManager.loadSounds();

        databaseManager = new DatabaseManager(getDataFolder());
        if (!databaseManager.connect()) {
            getLogger().severe("Database connection failed, plugin will be disabled");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        plotManager = new PlotManager(this, databaseManager, configManager);
        worldManager = new WorldManager(this, configManager);

        String defaultWorld = configManager.getDefaultWorld();
        if (Bukkit.getWorld(defaultWorld) == null) {
            worldManager.createPlotWorld(defaultWorld);
            getLogger().info("Default territory world '" + defaultWorld + "' created");
        }

        settingsMenu = new SettingsMenu(plotManager, configManager);
        plotMenu = new PlotMenu(settingsMenu, configManager);
        mainMenu = new MainMenu(plotManager, plotMenu, configManager);
        clearManager = new ClearManager(this, configManager);

        TerritoryCommand territoryCommand = new TerritoryCommand(this, plotManager, worldManager, configManager,
            messageManager, soundManager, mainMenu, plotMenu, clearManager);
        Objects.requireNonNull(getCommand("territory")).setExecutor(territoryCommand);
        Objects.requireNonNull(getCommand("territory")).setTabCompleter(territoryCommand);

        TerritoryAdminCommand territoryAdminCommand = new TerritoryAdminCommand(this, plotManager, worldManager,
            configManager, messageManager, soundManager);
        Objects.requireNonNull(getCommand("territoryadmin")).setExecutor(territoryAdminCommand);
        Objects.requireNonNull(getCommand("territoryadmin")).setTabCompleter(territoryAdminCommand);

        Bukkit.getPluginManager().registerEvents(
            new PlotProtectListener(this, plotManager, worldManager, configManager, messageManager, soundManager), this);
        Bukkit.getPluginManager().registerEvents(
            new PlayerListener(this, plotManager, worldManager, configManager, messageManager, soundManager), this);
        Bukkit.getPluginManager().registerEvents(
            new MenuListener(this, plotManager, plotMenu, mainMenu, settingsMenu, messageManager, worldManager, soundManager, clearManager), this);

        getLogger().info("YudxxTerritory v" + getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("YudxxTerritory disabled");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlotManager getPlotManager() {
        return plotManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }
}