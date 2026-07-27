package yudxx.minecraft.spigot.yudxxTerritory.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import yudxx.minecraft.spigot.yudxxTerritory.YudxxTerritory;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotData;
import yudxx.minecraft.spigot.yudxxTerritory.util.HexColorUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClearManager {

    private final YudxxTerritory plugin;
    private final ConfigManager configManager;
    private final Map<String, ClearSnapshot> snapshots;
    private final Map<String, Boolean> activeClears;

    public ClearManager(YudxxTerritory plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.snapshots = new ConcurrentHashMap<>();
        this.activeClears = new ConcurrentHashMap<>();
    }

    public void clearPlot(PlotData plot, Player player) {
        String key = plot.getPlotKey();
        if (activeClears.containsKey(key)) {
            player.sendMessage(HexColorUtil.color("&#FF5555A clear operation is already in progress for this territory"));
            return;
        }
        activeClears.put(key, true);
        String worldName = plot.getWorldName();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            activeClears.remove(key);
            return;
        }
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        int totalSize = settings.getTotalSize();
        int plotSize = settings.getPlotSize();
        int plotHeight = settings.getPlotHeight();
        int startX = plot.getPlotX() * totalSize;
        int startZ = plot.getPlotZ() * totalSize;
        int endX = startX + plotSize - 1;
        int endZ = startZ + plotSize - 1;
        Material fillMaterial = settings.getFillMaterial();

        Map<Location, Material> blockData = new HashMap<>();
        List<Location> blocksToClear = new ArrayList<>();
        List<Location> floorToRestore = new ArrayList<>();

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = 65; y <= 65 + plotHeight; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        blockData.put(block.getLocation().clone(), block.getType());
                        blocksToClear.add(block.getLocation().clone());
                    }
                }
                Block floor = world.getBlockAt(x, 64, z);
                if (floor.getType() != fillMaterial) {
                    blockData.put(floor.getLocation().clone(), floor.getType());
                    floorToRestore.add(floor.getLocation().clone());
                }
            }
        }

        if (blocksToClear.isEmpty() && floorToRestore.isEmpty()) {
            activeClears.remove(key);
            player.sendMessage(HexColorUtil.color("&#55FF55Territory already in original state"));
            return;
        }

        ClearSnapshot snapshot = new ClearSnapshot(blockData, System.currentTimeMillis());
        snapshots.put(key, snapshot);

        int total = blocksToClear.size() + floorToRestore.size();
        int batchSize = 80;
        int timeoutSec = configManager.getUndoTimeoutSeconds();
        player.sendMessage(HexColorUtil.color("&#55FF55Clearing " + total + " blocks... Use &#FFAA00/territory undo &#55FF55within " + timeoutSec + "s to undo"));

        new BukkitRunnable() {
            int airIndex = 0;
            int floorIndex = 0;
            int airTotal = blocksToClear.size();
            int floorTotal = floorToRestore.size();

            @Override
            public void run() {
                int processed = 0;
                while (airIndex < airTotal && processed < batchSize) {
                    Location loc = blocksToClear.get(airIndex);
                    Block block = world.getBlockAt(loc);
                    block.setType(Material.AIR);
                    airIndex++;
                    processed++;
                }
                while (floorIndex < floorTotal && processed < batchSize) {
                    Location loc = floorToRestore.get(floorIndex);
                    Block block = world.getBlockAt(loc);
                    block.setType(fillMaterial);
                    floorIndex++;
                    processed++;
                }
                if (airIndex >= airTotal && floorIndex >= floorTotal) {
                    activeClears.remove(key);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void undoClear(PlotData plot, Player player) {
        String key = plot.getPlotKey();
        if (activeClears.containsKey(key)) {
            player.sendMessage(HexColorUtil.color("&#FF5555A clear operation is in progress, cannot undo yet"));
            return;
        }
        ClearSnapshot snapshot = snapshots.get(key);
        if (snapshot == null) {
            player.sendMessage(HexColorUtil.color("&#FF5555No clear operation to undo for this territory"));
            return;
        }
        long elapsed = System.currentTimeMillis() - snapshot.timestamp;
        int timeoutMs = configManager.getUndoTimeoutSeconds() * 1000;
        if (elapsed > timeoutMs) {
            snapshots.remove(key);
            player.sendMessage(HexColorUtil.color("&#FF5555Undo timeout expired"));
            return;
        }
        int remainingSec = (int) ((timeoutMs - elapsed) / 1000);
        activeClears.put(key, true);
        String worldName = plot.getWorldName();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            activeClears.remove(key);
            snapshots.remove(key);
            return;
        }

        List<Map.Entry<Location, Material>> entries = new ArrayList<>(snapshot.blockData.entrySet());
        int total = entries.size();
        int batchSize = 80;
        player.sendMessage(HexColorUtil.color("&#55FF55Restoring " + total + " blocks... (&#FFAA00" + remainingSec + "s &#55FF55remaining)"));

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int batchEnd = Math.min(index + batchSize, total);
                for (int i = index; i < batchEnd; i++) {
                    Map.Entry<Location, Material> entry = entries.get(i);
                    Block block = world.getBlockAt(entry.getKey());
                    block.setType(entry.getValue());
                }
                index = batchEnd;
                if (index >= total) {
                    activeClears.remove(key);
                    snapshots.remove(key);
                    player.sendMessage(HexColorUtil.color("&#55FF55Undo complete! All blocks restored"));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void deletePlot(PlotData plot, Player player) {
        String key = plot.getPlotKey();
        if (activeClears.containsKey(key)) {
            player.sendMessage(HexColorUtil.color("&#FF5555An operation is already in progress for this territory"));
            return;
        }
        activeClears.put(key, true);
        String worldName = plot.getWorldName();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            activeClears.remove(key);
            return;
        }
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        int totalSize = settings.getTotalSize();
        int plotSize = settings.getPlotSize();
        int plotHeight = settings.getPlotHeight();
        int roadRadius = settings.getRoadRadius();
        int startX = plot.getPlotX() * totalSize;
        int startZ = plot.getPlotZ() * totalSize;
        int endX = startX + plotSize - 1;
        int endZ = startZ + plotSize - 1;
        int clearStartX = startX - roadRadius;
        int clearEndX = endX + roadRadius;
        int clearStartZ = startZ - roadRadius;
        int clearEndZ = endZ + roadRadius;

        List<Location> blocksToClear = new ArrayList<>();
        for (int x = clearStartX; x <= clearEndX; x++) {
            for (int z = clearStartZ; z <= clearEndZ; z++) {
                for (int y = 65; y <= 65 + plotHeight; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        blocksToClear.add(block.getLocation().clone());
                    }
                }
                Block floor = world.getBlockAt(x, 64, z);
                if (floor.getType() != Material.AIR) {
                    blocksToClear.add(floor.getLocation().clone());
                }
            }
        }

        int total = blocksToClear.size();
        int batchSize = 80;
        player.sendMessage(HexColorUtil.color("&#55FF55Deleting territory, clearing " + total + " blocks..."));

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int batchEnd = Math.min(index + batchSize, total);
                for (int i = index; i < batchEnd; i++) {
                    Location loc = blocksToClear.get(i);
                    Block block = world.getBlockAt(loc);
                    block.setType(Material.AIR);
                }
                index = batchEnd;
                if (index >= total) {
                    activeClears.remove(key);
                    snapshots.remove(key);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void removeSnapshot(String key) {
        snapshots.remove(key);
        activeClears.remove(key);
    }

    public boolean hasSnapshot(String key) {
        return snapshots.containsKey(key);
    }

    private static class ClearSnapshot {
        final Map<Location, Material> blockData;
        final long timestamp;

        ClearSnapshot(Map<Location, Material> blockData, long timestamp) {
            this.blockData = blockData;
            this.timestamp = timestamp;
        }
    }
}