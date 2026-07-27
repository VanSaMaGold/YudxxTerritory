package yudxx.minecraft.spigot.yudxxTerritory.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import yudxx.minecraft.spigot.yudxxTerritory.YudxxTerritory;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotData;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotSettings;
import yudxx.minecraft.spigot.yudxxTerritory.gui.SettingsMenu;
import yudxx.minecraft.spigot.yudxxTerritory.manager.ConfigManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.PlotManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.SoundManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.WorldManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.MessageManager;
import yudxx.minecraft.spigot.yudxxTerritory.util.HexColorUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final YudxxTerritory plugin;
    private final PlotManager plotManager;
    private final WorldManager worldManager;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;
    private final Map<UUID, String> lastPlotKey;

    public PlayerListener(YudxxTerritory plugin, PlotManager plotManager, WorldManager worldManager,
                          ConfigManager configManager, MessageManager messageManager, SoundManager soundManager) {
        this.plugin = plugin;
        this.plotManager = plotManager;
        this.worldManager = worldManager;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
        this.lastPlotKey = new HashMap<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            messageManager.send(player, "welcome");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastPlotKey.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        if (!worldManager.isPlotWorld(event.getTo().getWorld().getName())) return;
        Player player = event.getPlayer();

        if (event.getTo().getY() < configManager.getVoidTeleportY()) {
            PlotData nearest = plotManager.findNearestPlot(event.getTo());
            if (nearest != null) {
                Location spawn = plotManager.getPlotSpawn(nearest.getWorldName(), nearest.getPlotX(), nearest.getPlotZ());
                if (spawn != null) {
                    player.teleport(spawn);
                    player.sendMessage(HexColorUtil.color("&#FFAA00You fell too low! Teleported to nearest territory"));
                    return;
                }
            }
        }

        PlotData fromPlot = plotManager.getPlotAtLocation(event.getFrom());
        PlotData toPlot = plotManager.getPlotAtLocation(event.getTo());
        String fromKey = fromPlot != null ? fromPlot.getPlotKey() : "road";
        String toKey = toPlot != null ? toPlot.getPlotKey() : "road";
        if (fromKey.equals(toKey)) return;
        if (plotManager.isInSameMergeGroup(fromPlot, toPlot)) return;
        if (toPlot != null && toPlot.isBanned(player.getUniqueId()) && !player.hasPermission("yudxxterritory.admin.bypass")) {
            List<PlotData> playerPlots = plotManager.getPlayerPlots(player.getUniqueId());
            if (!playerPlots.isEmpty()) {
                Random random = new Random();
                PlotData randomPlot = playerPlots.get(random.nextInt(playerPlots.size()));
                Location spawn = plotManager.getPlotSpawn(randomPlot.getWorldName(), randomPlot.getPlotX(), randomPlot.getPlotZ());
                if (spawn != null) {
                    player.teleport(spawn);
                    messageManager.send(player, "banned-from-plot", Bukkit.getOfflinePlayer(toPlot.getOwner()).getName());
                    return;
                }
            }
        }
        if (fromPlot != null && fromPlot.getSettings().getBoolean("leave-message")) {
            if (fromPlot.hasOwner()) {
                String owner = Bukkit.getOfflinePlayer(fromPlot.getOwner()).getName();
                String ownerName = owner != null ? owner : "???";
                String title = messageManager.getMessage("plot-leave-title", ownerName);
                String subtitle = messageManager.getMessage("plot-leave-subtitle", ownerName);
                player.sendTitle(title, subtitle, 10, 40, 10);
                soundManager.playSound(player, "plot-leave", 0.5f, 1.0f);
            }
        }
        if (toPlot != null && toPlot.getSettings().getBoolean("enter-message")) {
            if (toPlot.hasOwner()) {
                String owner = Bukkit.getOfflinePlayer(toPlot.getOwner()).getName();
                String ownerName = owner != null ? owner : "???";
                String title = messageManager.getMessage("plot-enter-title", ownerName);
                String subtitle = messageManager.getMessage("plot-enter-subtitle", ownerName);
                player.sendTitle(title, subtitle, 10, 40, 10);
                soundManager.playSound(player, "plot-enter", 0.5f, 1.0f);
            }
        }
        applyPlotSettings(player, toPlot);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) return;
        if (!worldManager.isPlotWorld(event.getTo().getWorld().getName())) return;
        PlotData plot = plotManager.getPlotAtLocation(event.getTo());
        applyPlotSettings(event.getPlayer(), plot);
    }

    private void applyPlotSettings(Player player, PlotData plot) {
        if (plot == null) {
            resetPlayerSettings(player);
            return;
        }
        SettingsMenu.applySinglePlayerSettings(player, plot.getSettings());
    }

    private void resetPlayerSettings(Player player) {
        player.resetPlayerTime();
        player.resetPlayerWeather();
        if (!player.hasPermission("yudxxterritory.admin.bypass")) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }
}