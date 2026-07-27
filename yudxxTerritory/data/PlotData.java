package yudxx.minecraft.spigot.yudxxTerritory.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlotData {

    private final int id;
    private final String worldName;
    private final int plotX;
    private final int plotZ;
    private UUID owner;
    private String name;
    private final List<UUID> trusted;
    private final List<UUID> banned;
    private final List<String> mergedPlots;
    private boolean merged;
    private PlotSettings settings;

    public PlotData(int id, String worldName, int plotX, int plotZ) {
        this.id = id;
        this.worldName = worldName;
        this.plotX = plotX;
        this.plotZ = plotZ;
        this.owner = null;
        this.name = null;
        this.trusted = new ArrayList<>();
        this.banned = new ArrayList<>();
        this.mergedPlots = new ArrayList<>();
        this.merged = false;
        this.settings = new PlotSettings();
    }

    public int getId() {
        return id;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getPlotX() {
        return plotX;
    }

    public int getPlotZ() {
        return plotZ;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public boolean hasOwner() {
        return owner != null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return name != null ? name : (worldName + " (" + plotX + ", " + plotZ + ")");
    }

    public List<UUID> getTrusted() {
        return trusted;
    }

    public void addTrusted(UUID uuid) {
        if (!trusted.contains(uuid)) {
            trusted.add(uuid);
        }
    }

    public void removeTrusted(UUID uuid) {
        trusted.remove(uuid);
    }

    public boolean isTrusted(UUID uuid) {
        return trusted.contains(uuid);
    }

    public List<UUID> getBanned() {
        return banned;
    }

    public void addBanned(UUID uuid) {
        if (!banned.contains(uuid)) {
            banned.add(uuid);
        }
    }

    public void removeBanned(UUID uuid) {
        banned.remove(uuid);
    }

    public boolean isBanned(UUID uuid) {
        return banned.contains(uuid);
    }

    public List<String> getMergedPlots() {
        return mergedPlots;
    }

    public void addMergedPlot(String key) {
        if (!mergedPlots.contains(key)) {
            mergedPlots.add(key);
        }
    }

    public void removeMergedPlot(String key) {
        mergedPlots.remove(key);
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    public PlotSettings getSettings() {
        return settings;
    }

    public void setSettings(PlotSettings settings) {
        this.settings = settings;
    }

    public String getPlotKey() {
        return worldName + ":" + plotX + ":" + plotZ;
    }

    public boolean isOwner(UUID uuid) {
        return owner != null && owner.equals(uuid);
    }

    public boolean canBuild(UUID uuid) {
        if (owner == null) {
            return uuid != null;
        }
        if (owner.equals(uuid)) {
            return true;
        }
        return trusted.contains(uuid);
    }

    public void clear() {
        name = null;
        trusted.clear();
        banned.clear();
        mergedPlots.clear();
        merged = false;
        settings = new PlotSettings();
    }
}