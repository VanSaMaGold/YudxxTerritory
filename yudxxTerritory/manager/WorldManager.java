package yudxx.minecraft.spigot.yudxxTerritory.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import yudxx.minecraft.spigot.yudxxTerritory.YudxxTerritory;
import yudxx.minecraft.spigot.yudxxTerritory.util.HexColorUtil;

import java.util.ArrayList;
import java.util.List;

public class WorldManager {

    private final YudxxTerritory plugin;
    private final ConfigManager configManager;

    public WorldManager(YudxxTerritory plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void createPlotWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) return;
        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.createWorld();
    }

    public void generatePlotWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        if (settings.isInfinite()) return;
        int totalSize = settings.getTotalSize();
        int plotSize = settings.getPlotSize();
        int maxRange = settings.getMaxRange();
        int baseY = 64;
        Material roadMaterial = settings.getRoadMaterial();
        Material fillMaterial = settings.getFillMaterial();
        int roadRadius = settings.getRoadRadius();
        for (int px = -maxRange; px <= maxRange; px++) {
            for (int pz = -maxRange; pz <= maxRange; pz++) {
                int startX = px * totalSize;
                int startZ = pz * totalSize;
                int endX = startX + plotSize - 1;
                int endZ = startZ + plotSize - 1;
                int roadStartX = startX - roadRadius;
                int roadEndX = endX + roadRadius;
                int roadStartZ = startZ - roadRadius;
                int roadEndZ = endZ + roadRadius;
                for (int x = roadStartX; x <= roadEndX; x++) {
                    for (int z = roadStartZ; z <= roadEndZ; z++) {
                        boolean isPlot = x >= startX && x <= endX && z >= startZ && z <= endZ;
                        boolean isRoad = !isPlot;
                        for (int y = 0; y <= baseY; y++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (y == baseY) {
                                block.setType(isRoad ? roadMaterial : fillMaterial);
                            } else if (y >= baseY - 4) {
                                block.setType(Material.DIRT);
                            } else {
                                block.setType(Material.STONE);
                            }
                        }
                    }
                }
            }
        }
    }

    public void replaceBorderMaterial(String worldName, Material newMaterial, CommandSender sender) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(HexColorUtil.color("&#FF5555World not found"));
            return;
        }
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        configManager.setWorldBorderMaterial(worldName, newMaterial);
        Material oldMaterial = settings.getBorderMaterial();
        int totalSize = settings.getTotalSize();
        int plotSize = settings.getPlotSize();
        int baseY = 64;

        List<Location> blocksToReplace = new ArrayList<>();
        if (settings.isInfinite()) {
            int searchRadius = 50;
            for (int px = -searchRadius; px <= searchRadius; px++) {
                for (int pz = -searchRadius; pz <= searchRadius; pz++) {
                    collectBorderBlocks(world, px, pz, totalSize, plotSize, baseY, oldMaterial, blocksToReplace);
                }
            }
        } else {
            int maxRange = settings.getMaxRange();
            for (int px = -maxRange; px <= maxRange; px++) {
                for (int pz = -maxRange; pz <= maxRange; pz++) {
                    collectBorderBlocks(world, px, pz, totalSize, plotSize, baseY, oldMaterial, blocksToReplace);
                }
            }
        }

        if (blocksToReplace.isEmpty()) {
            sender.sendMessage(HexColorUtil.color("&#55FF55No border blocks found to replace"));
            return;
        }

        int total = blocksToReplace.size();
        int batchSize = 100;
        long startTime = System.currentTimeMillis();
        sender.sendMessage(HexColorUtil.color("&#55FF55Replacing " + total + " border blocks with " + newMaterial.name() + "..."));

        new BukkitRunnable() {
            int index = 0;
            int lastReported = 0;

            @Override
            public void run() {
                int batchEnd = Math.min(index + batchSize, total);
                for (int i = index; i < batchEnd; i++) {
                    Location loc = blocksToReplace.get(i);
                    Block block = world.getBlockAt(loc);
                    block.setType(newMaterial);
                }
                index = batchEnd;
                if (index - lastReported >= 500 || index >= total) {
                    lastReported = index;
                    int percent = (int) ((double) index / total * 100);
                    sender.sendMessage(HexColorUtil.color("&#AAAAAAProgress: &#FFAA00" + percent + "% &#AAAAAA(" + index + "/" + total + ")"));
                }
                if (index >= total) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    sender.sendMessage(HexColorUtil.color("&#55FF55Border material replaced! Took " + (elapsed / 1000.0) + "s"));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void collectBorderBlocks(World world, int px, int pz, int totalSize, int plotSize, int baseY,
                                      Material oldMaterial, List<Location> blocks) {
        int startX = px * totalSize;
        int startZ = pz * totalSize;
        int endX = startX + plotSize - 1;
        int endZ = startZ + plotSize - 1;
        for (int x = startX - 1; x <= endX + 1; x++) {
            for (int z = startZ - 1; z <= endZ + 1; z++) {
                boolean isBorder = (x == startX - 1 || x == endX + 1) && (z >= startZ - 1 && z <= endZ + 1);
                isBorder = isBorder || ((z == startZ - 1 || z == endZ + 1) && (x >= startX - 1 && x <= endX + 1));
                if (isBorder) {
                    Block block = world.getBlockAt(x, baseY, z);
                    if (block.getType() == oldMaterial) {
                        blocks.add(block.getLocation());
                    }
                }
            }
        }
    }

    public void fillPlotInterior(String worldName, int plotX, int plotZ) {
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
        int baseY = 64;
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                Block block = world.getBlockAt(x, baseY, z);
                if (block.getType() == Material.AIR || block.getType() == Material.VOID_AIR) {
                    block.setType(fillMaterial);
                }
            }
        }
    }

    public void generatePlotRoads(String worldName, int plotX, int plotZ) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        int totalSize = settings.getTotalSize();
        int plotSize = settings.getPlotSize();
        int roadRadius = settings.getRoadRadius();
        int startX = plotX * totalSize;
        int startZ = plotZ * totalSize;
        int endX = startX + plotSize - 1;
        int endZ = startZ + plotSize - 1;
        int roadStartX = startX - roadRadius;
        int roadEndX = endX + roadRadius;
        int roadStartZ = startZ - roadRadius;
        int roadEndZ = endZ + roadRadius;
        Material roadMaterial = settings.getRoadMaterial();
        int baseY = 64;
        for (int x = roadStartX; x <= roadEndX; x++) {
            for (int z = roadStartZ; z <= roadEndZ; z++) {
                boolean isPlot = x >= startX && x <= endX && z >= startZ && z <= endZ;
                if (isPlot) continue;
                for (int y = 0; y <= baseY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (y == baseY) {
                        block.setType(roadMaterial);
                    } else if (y >= baseY - 4) {
                        block.setType(Material.DIRT);
                    } else {
                        block.setType(Material.STONE);
                    }
                }
            }
        }
    }

    public void clearPlotInteriorProgressive(String worldName, int plotX, int plotZ, CommandSender sender) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        int totalSize = settings.getTotalSize();
        int plotSize = settings.getPlotSize();
        int plotHeight = settings.getPlotHeight();
        int startX = plotX * totalSize;
        int startZ = plotZ * totalSize;
        int endX = startX + plotSize - 1;
        int endZ = startZ + plotSize - 1;
        Material fillMaterial = settings.getFillMaterial();

        List<Location> blocksToClear = new ArrayList<>();
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = 65; y <= 65 + plotHeight; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        blocksToClear.add(block.getLocation());
                    }
                }
                Block floor = world.getBlockAt(x, 64, z);
                if (floor.getType() != fillMaterial && floor.getType() != Material.AIR) {
                    floor.setType(fillMaterial);
                }
            }
        }

        if (blocksToClear.isEmpty()) {
            if (sender != null) {
                sender.sendMessage(HexColorUtil.color("&#55FF55Territory already cleared"));
            }
            return;
        }

        int total = blocksToClear.size();
        int batchSize = 80;
        if (sender != null) {
            sender.sendMessage(HexColorUtil.color("&#55FF55Clearing " + total + " blocks..."));
        }

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
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void teleportToPlotSpawn(Player player) {
        Location spawn = getPlotWorldSpawn(configManager.getDefaultWorld());
        if (spawn != null) {
            player.teleport(spawn);
        }
    }

    public Location getPlotWorldSpawn(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        int totalSize = settings.getTotalSize();
        double x = totalSize / 2.0;
        double z = totalSize / 2.0;
        return new Location(world, x + 0.5, 65, z + 0.5);
    }

    public boolean isPlotWorld(String worldName) {
        return configManager.getWorldSettings(worldName).isEnabled();
    }

    public boolean isWorldFull(String worldName) {
        ConfigManager.WorldSettings settings = configManager.getWorldSettings(worldName);
        if (settings.isInfinite()) return false;
        int maxRange = settings.getMaxRange();
        int totalPlots = (2 * maxRange + 1) * (2 * maxRange + 1);
        int claimedPlots = plugin.getDatabaseManager().getPlotCountInWorld(worldName);
        return claimedPlots >= totalPlots;
    }
}