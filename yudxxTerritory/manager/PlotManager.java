package yudxx.minecraft.spigot.yudxxTerritory.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import yudxx.minecraft.spigot.yudxxTerritory.YudxxTerritory;
import yudxx.minecraft.spigot.yudxxTerritory.data.DatabaseManager;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotData;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlotManager {

    private final YudxxTerritory plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final Map<String, PlotData> plotCache;

    public PlotManager(YudxxTerritory plugin, DatabaseManager databaseManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
        this.plotCache = new HashMap<>();
    }

    public PlotData getPlot(String world, int plotX, int plotZ) {
        String key = world + ":" + plotX + ":" + plotZ;
        if (plotCache.containsKey(key)) {
            return plotCache.get(key);
        }
        PlotData plot = databaseManager.getPlot(world, plotX, plotZ);
        if (plot != null) {
            plotCache.put(key, plot);
        }
        return plot;
    }

    public PlotData getOrCreatePlot(String world, int plotX, int plotZ) {
        PlotData plot = getPlot(world, plotX, plotZ);
        if (plot == null) {
            plot = databaseManager.getOrCreatePlot(world, plotX, plotZ);
            if (plot != null) {
                plotCache.put(plot.getPlotKey(), plot);
            }
        }
        return plot;
    }

    public PlotData getPlotAtLocation(Location location) {
        if (location.getWorld() == null) return null;
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(location.getWorld().getName());
        int totalSize = settings.getTotalSize();
        int plotX = Math.floorDiv(location.getBlockX(), totalSize);
        int plotZ = Math.floorDiv(location.getBlockZ(), totalSize);
        double offsetX = location.getBlockX() - (plotX * totalSize);
        double offsetZ = location.getBlockZ() - (plotZ * totalSize);
        if (offsetX < 0 || offsetX >= settings.getPlotSize() || offsetZ < 0 || offsetZ >= settings.getPlotSize()) {
            return null;
        }
        return getPlot(location.getWorld().getName(), plotX, plotZ);
    }

    public Location getPlotCenter(String worldName, int plotX, int plotZ) {
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        int totalSize = settings.getTotalSize();
        double centerX = plotX * totalSize + settings.getPlotSize() / 2.0;
        double centerZ = plotZ * totalSize + settings.getPlotSize() / 2.0;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, centerX, 64, centerZ);
    }

    public Location getPlotSpawn(String worldName, int plotX, int plotZ) {
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        int totalSize = settings.getTotalSize();
        double centerX = plotX * totalSize + settings.getPlotSize() / 2.0;
        double centerZ = plotZ * totalSize + settings.getPlotSize() / 2.0;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        int y = world.getHighestBlockYAt((int) centerX, (int) centerZ);
        return new Location(world, centerX + 0.5, y + 1, centerZ + 0.5);
    }

    public Location getPlotRoadCenter(String worldName, int plotX, int plotZ) {
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        int totalSize = settings.getTotalSize();
        double centerX = plotX * totalSize + settings.getPlotSize() / 2.0;
        double centerZ = plotZ * totalSize + settings.getPlotSize() / 2.0;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, centerX + 0.5, 64, centerZ + 0.5);
    }

    public int getMaxPlotsForPlayer(Player player) {
        if (player.hasPermission("yudxxterritory.plot.limit.unlimited")) return Integer.MAX_VALUE;
        if (player.hasPermission("yudxxterritory.plot.limit.10")) return 10;
        if (player.hasPermission("yudxxterritory.plot.limit.5")) return 5;
        if (player.hasPermission("yudxxterritory.plot.limit.3")) return 3;
        if (player.hasPermission("yudxxterritory.plot.limit.1")) return 1;
        return configManager.getMaxPlotsPerPlayer();
    }

    public boolean claimPlot(Player player, String world, int plotX, int plotZ) {
        PlotData plot = getOrCreatePlot(world, plotX, plotZ);
        if (plot == null) return false;
        if (plot.hasOwner()) return false;
        List<PlotData> playerPlots = databaseManager.getPlayerPlots(player.getUniqueId());
        int maxPlots = getMaxPlotsForPlayer(player);
        if (playerPlots.size() >= maxPlots) return false;
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(world);
        if (!settings.isInfinite()) {
            if (Math.abs(plotX) > settings.getMaxRange() || Math.abs(plotZ) > settings.getMaxRange()) return false;
        }
        plot.setOwner(player.getUniqueId());
        databaseManager.setOwner(plot.getId(), player.getUniqueId());
        WorldManager worldManager = plugin.getWorldManager();
        worldManager.fillPlotInterior(world, plotX, plotZ);
        worldManager.generatePlotRoads(world, plotX, plotZ);
        generatePlotBorder(world, plotX, plotZ);
        return true;
    }

    public boolean autoClaim(Player player) {
        String currentWorld = player.getWorld().getName();
        List<String> plotWorlds = configManager.getPlotWorlds();
        int startIndex = plotWorlds.indexOf(currentWorld);
        if (startIndex < 0) startIndex = 0;
        for (int wi = startIndex; wi < plotWorlds.size(); wi++) {
            String worldName = plotWorlds.get(wi);
            if (plugin.getWorldManager().isWorldFull(worldName)) continue;
            ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
            if (!settings.isEnabled()) continue;
            if (settings.isInfinite()) {
                int safetyLimit = 100000;
                int checked = 0;
                for (int r = 0; checked < safetyLimit; r++) {
                    for (int x = -r; x <= r; x++) {
                        for (int z = -r; z <= r; z++) {
                            if (Math.abs(x) != r && Math.abs(z) != r) continue;
                            checked++;
                            PlotData plot = getPlot(worldName, x, z);
                            if (plot == null || !plot.hasOwner()) {
                                return claimPlot(player, worldName, x, z);
                            }
                        }
                    }
                }
                continue;
            }
            int maxRange = settings.getMaxRange();
            for (int r = 0; r <= maxRange; r++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.abs(x) != r && Math.abs(z) != r) continue;
                        PlotData plot = getPlot(worldName, x, z);
                        if (plot == null || !plot.hasOwner()) {
                            return claimPlot(player, worldName, x, z);
                        }
                    }
                }
            }
        }
        for (int wi = 0; wi < startIndex; wi++) {
            String worldName = plotWorlds.get(wi);
            if (plugin.getWorldManager().isWorldFull(worldName)) continue;
            ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
            if (!settings.isEnabled()) continue;
            if (settings.isInfinite()) {
                int safetyLimit = 100000;
                int checked = 0;
                for (int r = 0; checked < safetyLimit; r++) {
                    for (int x = -r; x <= r; x++) {
                        for (int z = -r; z <= r; z++) {
                            if (Math.abs(x) != r && Math.abs(z) != r) continue;
                            checked++;
                            PlotData plot = getPlot(worldName, x, z);
                            if (plot == null || !plot.hasOwner()) {
                                return claimPlot(player, worldName, x, z);
                            }
                        }
                    }
                }
                continue;
            }
            int maxRange = settings.getMaxRange();
            for (int r = 0; r <= maxRange; r++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.abs(x) != r && Math.abs(z) != r) continue;
                        PlotData plot = getPlot(worldName, x, z);
                        if (plot == null || !plot.hasOwner()) {
                            return claimPlot(player, worldName, x, z);
                        }
                    }
                }
            }
        }
        return false;
    }

    public void generatePlotBorder(String worldName, int plotX, int plotZ) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        int totalSize = settings.getTotalSize();
        int plotSize = settings.getPlotSize();
        int startX = plotX * totalSize;
        int startZ = plotZ * totalSize;
        int endX = startX + plotSize - 1;
        int endZ = startZ + plotSize - 1;
        Material borderMaterial = settings.getBorderMaterial();
        for (int x = startX - 1; x <= endX + 1; x++) {
            for (int z = startZ - 1; z <= endZ + 1; z++) {
                boolean isBorder = (x == startX - 1 || x == endX + 1) && (z >= startZ - 1 && z <= endZ + 1);
                isBorder = isBorder || ((z == startZ - 1 || z == endZ + 1) && (x >= startX - 1 && x <= endX + 1));
                if (isBorder) {
                    Block block = world.getBlockAt(x, 64, z);
                    block.setType(borderMaterial);
                    Block topBlock = world.getBlockAt(x, 65, z);
                    topBlock.setType(borderMaterial);
                }
            }
        }
    }

    public boolean isBorderBlock(Location location) {
        if (location.getWorld() == null) return false;
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(location.getWorld().getName());
        int totalSize = settings.getTotalSize();
        int plotSize = settings.getPlotSize();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int y = location.getBlockY();
        int plotX = Math.floorDiv(x, totalSize);
        int plotZ = Math.floorDiv(z, totalSize);
        int startX = plotX * totalSize;
        int startZ = plotZ * totalSize;
        int endX = startX + plotSize - 1;
        int endZ = startZ + plotSize - 1;
        boolean isBorderX = (x == startX - 1 || x == endX + 1) && (z >= startZ - 1 && z <= endZ + 1);
        boolean isBorderZ = (z == startZ - 1 || z == endZ + 1) && (x >= startX - 1 && x <= endX + 1);
        if ((y == 64 || y == 65) && (isBorderX || isBorderZ)) {
            PlotData plot = getPlot(location.getWorld().getName(), plotX, plotZ);
            if (plot != null && plot.isMerged()) {
                List<PlotData> group = getMergeGroup(plot);
                int minPlotX = Integer.MAX_VALUE, minPlotZ = Integer.MAX_VALUE;
                int maxPlotX = Integer.MIN_VALUE, maxPlotZ = Integer.MIN_VALUE;
                for (PlotData p : group) {
                    minPlotX = Math.min(minPlotX, p.getPlotX());
                    minPlotZ = Math.min(minPlotZ, p.getPlotZ());
                    maxPlotX = Math.max(maxPlotX, p.getPlotX());
                    maxPlotZ = Math.max(maxPlotZ, p.getPlotZ());
                }
                int gStartX = minPlotX * totalSize;
                int gStartZ = minPlotZ * totalSize;
                int gEndX = maxPlotX * totalSize + plotSize - 1;
                int gEndZ = maxPlotZ * totalSize + plotSize - 1;
                boolean onOuterBorder = (x == gStartX - 1 || x == gEndX + 1) && (z >= gStartZ - 1 && z <= gEndZ + 1);
                onOuterBorder = onOuterBorder || ((z == gStartZ - 1 || z == gEndZ + 1) && (x >= gStartX - 1 && x <= gEndX + 1));
                return onOuterBorder;
            }
            return true;
        }
        return false;
    }

    public void clearPlotBlocks(String worldName, int plotX, int plotZ) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        int totalSize = settings.getTotalSize();
        int plotSize = settings.getPlotSize();
        int startX = plotX * totalSize;
        int startZ = plotZ * totalSize;
        int endX = startX + plotSize - 1;
        int endZ = startZ + plotSize - 1;
        Material fillMaterial = settings.getFillMaterial();
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = 65; y <= 65 + settings.getPlotHeight(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        block.setType(Material.AIR);
                    }
                }
                Block floor = world.getBlockAt(x, 64, z);
                if (floor.getType() != fillMaterial && floor.getType() != Material.AIR) {
                    floor.setType(fillMaterial);
                }
            }
        }
    }

    public void deletePlotData(PlotData plot) {
        databaseManager.deletePlot(plot.getId());
        plotCache.remove(plot.getPlotKey());
    }

    public void clearPlotData(PlotData plot) {
        databaseManager.clearPlot(plot.getId());
        plot.clear();
        plotCache.put(plot.getPlotKey(), plot);
    }

    public void trustPlayer(PlotData plot, UUID uuid) {
        plot.addTrusted(uuid);
        databaseManager.addTrusted(plot.getId(), uuid);
    }

    public void untrustPlayer(PlotData plot, UUID uuid) {
        plot.removeTrusted(uuid);
        databaseManager.removeTrusted(plot.getId(), uuid);
    }

    public void banPlayer(PlotData plot, UUID uuid) {
        plot.addBanned(uuid);
        databaseManager.addBanned(plot.getId(), uuid);
    }

    public void unbanPlayer(PlotData plot, UUID uuid) {
        plot.removeBanned(uuid);
        databaseManager.removeBanned(plot.getId(), uuid);
    }

    public void mergePlots(PlotData plot1, PlotData plot2) {
        plot1.setMerged(true);
        plot2.setMerged(true);
        plot1.addMergedPlot(plot2.getPlotKey());
        plot2.addMergedPlot(plot1.getPlotKey());
        databaseManager.setMerged(plot1.getId(), true);
        databaseManager.setMerged(plot2.getId(), true);
        databaseManager.addMerged(plot1.getId(), plot2.getPlotKey());
        databaseManager.addMerged(plot2.getId(), plot1.getPlotKey());
        regenerateMergedBorder(plot1);
    }

    public void unmergePlot(PlotData plot) {
        List<String> mergedKeys = new ArrayList<>(plot.getMergedPlots());
        for (String key : mergedKeys) {
            String[] parts = key.split(":");
            if (parts.length == 3) {
                PlotData other = getPlot(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                if (other != null) {
                    other.removeMergedPlot(plot.getPlotKey());
                    other.setMerged(false);
                    databaseManager.removeMerged(other.getId(), plot.getPlotKey());
                    databaseManager.setMerged(other.getId(), false);
                    plotCache.put(other.getPlotKey(), other);
                }
            }
            databaseManager.removeMerged(plot.getId(), key);
        }
        plot.getMergedPlots().clear();
        plot.setMerged(false);
        databaseManager.setMerged(plot.getId(), false);
        databaseManager.clearMerged(plot.getId());
        plotCache.put(plot.getPlotKey(), plot);
        generatePlotBorder(plot.getWorldName(), plot.getPlotX(), plot.getPlotZ());
        for (String key : mergedKeys) {
            String[] parts = key.split(":");
            if (parts.length == 3) {
                generatePlotBorder(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            }
        }
    }

    private void regenerateMergedBorder(PlotData plot) {
        List<PlotData> group = getMergeGroup(plot);
        if (group.size() < 2) return;
        String worldName = plot.getWorldName();
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        int totalSize = settings.getTotalSize();
        int plotSize = settings.getPlotSize();
        int minPlotX = Integer.MAX_VALUE, minPlotZ = Integer.MAX_VALUE;
        int maxPlotX = Integer.MIN_VALUE, maxPlotZ = Integer.MIN_VALUE;
        for (PlotData p : group) {
            minPlotX = Math.min(minPlotX, p.getPlotX());
            minPlotZ = Math.min(minPlotZ, p.getPlotZ());
            maxPlotX = Math.max(maxPlotX, p.getPlotX());
            maxPlotZ = Math.max(maxPlotZ, p.getPlotZ());
        }
        int worldStartX = minPlotX * totalSize;
        int worldStartZ = minPlotZ * totalSize;
        int worldEndX = maxPlotX * totalSize + plotSize - 1;
        int worldEndZ = maxPlotZ * totalSize + plotSize - 1;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        Material fillMaterial = settings.getFillMaterial();
        Material borderMaterial = settings.getBorderMaterial();
        for (int x = worldStartX; x <= worldEndX; x++) {
            for (int z = worldStartZ; z <= worldEndZ; z++) {
                Block block = world.getBlockAt(x, 64, z);
                block.setType(fillMaterial);
                Block topBlock = world.getBlockAt(x, 65, z);
                if (topBlock.getType() == borderMaterial) {
                    topBlock.setType(Material.AIR);
                }
            }
        }
        for (int x = worldStartX - 1; x <= worldEndX + 1; x++) {
            for (int z = worldStartZ - 1; z <= worldEndZ + 1; z++) {
                boolean isBorder = (x == worldStartX - 1 || x == worldEndX + 1) && (z >= worldStartZ - 1 && z <= worldEndZ + 1);
                isBorder = isBorder || ((z == worldStartZ - 1 || z == worldEndZ + 1) && (x >= worldStartX - 1 && x <= worldEndX + 1));
                if (isBorder) {
                    Block block = world.getBlockAt(x, 64, z);
                    block.setType(borderMaterial);
                    Block topBlock = world.getBlockAt(x, 65, z);
                    topBlock.setType(borderMaterial);
                }
            }
        }
    }

    private List<PlotData> getMergeGroup(PlotData plot) {
        List<PlotData> group = new ArrayList<>();
        group.add(plot);
        for (String key : plot.getMergedPlots()) {
            String[] parts = key.split(":");
            if (parts.length == 3) {
                PlotData other = getPlot(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                if (other != null) {
                    group.add(other);
                    for (String subKey : other.getMergedPlots()) {
                        String[] subParts = subKey.split(":");
                        if (subParts.length == 3) {
                            boolean alreadyInGroup = false;
                            for (PlotData g : group) {
                                if (g.getPlotKey().equals(subParts[0] + ":" + subParts[1] + ":" + subParts[2])) {
                                    alreadyInGroup = true;
                                    break;
                                }
                            }
                            if (!alreadyInGroup) {
                                PlotData sub = getPlot(subParts[0], Integer.parseInt(subParts[1]), Integer.parseInt(subParts[2]));
                                if (sub != null) group.add(sub);
                            }
                        }
                    }
                }
            }
        }
        return group;
    }

    public boolean isAdjacent(PlotData plot1, PlotData plot2) {
        if (!plot1.getWorldName().equals(plot2.getWorldName())) return false;
        int dx = Math.abs(plot1.getPlotX() - plot2.getPlotX());
        int dz = Math.abs(plot1.getPlotZ() - plot2.getPlotZ());
        return (dx == 1 && dz == 0) || (dx == 0 && dz == 1);
    }

    public boolean isInSameMergeGroup(PlotData plot1, PlotData plot2) {
        if (plot1 == null || plot2 == null) return false;
        if (plot1.getPlotKey().equals(plot2.getPlotKey())) return true;
        if (!plot1.isMerged() || !plot2.isMerged()) return false;
        List<PlotData> group = getMergeGroup(plot1);
        for (PlotData p : group) {
            if (p.getPlotKey().equals(plot2.getPlotKey())) return true;
        }
        return false;
    }

    public PlotData getPlayerPlotInWorld(UUID uuid, String world) {
        List<PlotData> plots = databaseManager.getPlayerPlots(uuid);
        for (PlotData plot : plots) {
            if (plot.getWorldName().equals(world)) return plot;
        }
        return null;
    }

    public List<PlotData> getPlayerPlots(UUID uuid) {
        return databaseManager.getPlayerPlots(uuid);
    }

    public void savePlotSettings(PlotData plot) {
        databaseManager.saveSettings(plot.getId(), plot.getSettings());
    }

    public void setPlotName(PlotData plot, String name) {
        plot.setName(name);
        databaseManager.setName(plot.getId(), name);
    }

    public void invalidateCache(String key) {
        plotCache.remove(key);
    }

    public PlotData findNearestPlot(Location location) {
        if (location.getWorld() == null) return null;
        String worldName = location.getWorld().getName();
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        int totalSize = settings.getTotalSize();
        int px = Math.floorDiv(location.getBlockX(), totalSize);
        int pz = Math.floorDiv(location.getBlockZ(), totalSize);
        PlotData nearest = null;
        double nearestDist = Double.MAX_VALUE;
        int searchRadius = 10;
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                PlotData plot = getPlot(worldName, px + dx, pz + dz);
                if (plot != null && plot.hasOwner()) {
                    Location center = getPlotCenter(worldName, plot.getPlotX(), plot.getPlotZ());
                    if (center != null) {
                        double dist = location.distanceSquared(center);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = plot;
                        }
                    }
                }
            }
        }
        return nearest;
    }
}