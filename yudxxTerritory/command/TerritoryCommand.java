package yudxx.minecraft.spigot.yudxxTerritory.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import yudxx.minecraft.spigot.yudxxTerritory.YudxxTerritory;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotData;
import yudxx.minecraft.spigot.yudxxTerritory.gui.MainMenu;
import yudxx.minecraft.spigot.yudxxTerritory.gui.PlotMenu;
import yudxx.minecraft.spigot.yudxxTerritory.manager.ClearManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.ConfigManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.PlotManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.SoundManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.WorldManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.MessageManager;
import yudxx.minecraft.spigot.yudxxTerritory.util.HexColorUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TerritoryCommand implements CommandExecutor, TabCompleter {

    private final YudxxTerritory plugin;
    private final PlotManager plotManager;
    private final WorldManager worldManager;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;
    private final MainMenu mainMenu;
    private final PlotMenu plotMenu;
    private final ClearManager clearManager;

    private static final int HELP_PER_PAGE = 5;

    private static final List<String[]> HELP_ENTRIES = Arrays.asList(
        new String[]{"auto", "/territory auto", "Auto-claim a territory"},
        new String[]{"claim", "/territory claim", "Claim the territory you are standing on"},
        new String[]{"home", "/territory home [index]", "Teleport to your territory"},
        new String[]{"visit", "/territory visit <player>", "Visit another player''s territory"},
        new String[]{"info", "/territory info", "View current territory info"},
        new String[]{"setname", "/territory setname <name>", "Set a name for your territory"},
        new String[]{"trust", "/territory trust <player>", "Trust a player on your territory"},
        new String[]{"untrust", "/territory untrust <player>", "Remove trust from a player"},
        new String[]{"merge", "/territory merge", "Merge with adjacent territory"},
        new String[]{"unmerge", "/territory unmerge", "Cancel all merges"},
        new String[]{"clear", "/territory clear", "Clear your territory blocks"},
        new String[]{"undo", "/territory undo", "Undo the last clear operation"},
        new String[]{"delete", "/territory delete", "Delete your territory"},
        new String[]{"middle", "/territory middle", "Teleport to territory center"},
        new String[]{"kick", "/territory kick <player>", "Kick a player from your territory"},
        new String[]{"settings", "/territory settings", "Open territory settings menu"},
        new String[]{"list", "/territory list", "View your territory list"},
        new String[]{"gui", "/territory gui", "Open territory main menu"},
        new String[]{"ban", "/territory ban <player>", "Ban a player from your territory"},
        new String[]{"unban", "/territory unban <player>", "Unban a player from your territory"}
    );

    public TerritoryCommand(YudxxTerritory plugin, PlotManager plotManager, WorldManager worldManager,
                            ConfigManager configManager, MessageManager messageManager,
                            SoundManager soundManager, MainMenu mainMenu, PlotMenu plotMenu,
                            ClearManager clearManager) {
        this.plugin = plugin;
        this.plotManager = plotManager;
        this.worldManager = worldManager;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
        this.mainMenu = mainMenu;
        this.plotMenu = plotMenu;
        this.clearManager = clearManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messageManager.send(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            sendHelp(player, 1);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help":
                int page = 1;
                if (args.length >= 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        messageManager.send(player, "invalid-number");
                        return true;
                    }
                }
                sendHelp(player, page);
                break;
            case "auto":
                checkPermissionOrDeny(player, "yudxxterritory.command.auto", () -> handleAuto(player));
                break;
            case "claim":
                checkPermissionOrDeny(player, "yudxxterritory.command.claim", () -> handleClaim(player));
                break;
            case "home":
                checkPermissionOrDeny(player, "yudxxterritory.command.home", () -> handleHome(player, args));
                break;
            case "visit":
                checkPermissionOrDeny(player, "yudxxterritory.command.visit", () -> handleVisit(player, args));
                break;
            case "info":
                checkPermissionOrDeny(player, "yudxxterritory.command.info", () -> handleInfo(player));
                break;
            case "setname":
                checkPermissionOrDeny(player, "yudxxterritory.command.setname", () -> handleSetName(player, args));
                break;
            case "trust":
                checkPermissionOrDeny(player, "yudxxterritory.command.trust", () -> handleTrust(player, args));
                break;
            case "untrust":
                checkPermissionOrDeny(player, "yudxxterritory.command.untrust", () -> handleUntrust(player, args));
                break;
            case "merge":
                checkPermissionOrDeny(player, "yudxxterritory.command.merge", () -> handleMerge(player));
                break;
            case "unmerge":
                checkPermissionOrDeny(player, "yudxxterritory.command.unmerge", () -> handleUnmerge(player));
                break;
            case "clear":
                checkPermissionOrDeny(player, "yudxxterritory.command.clear", () -> handleClear(player));
                break;
            case "undo":
                checkPermissionOrDeny(player, "yudxxterritory.command.undo", () -> handleUndo(player));
                break;
            case "delete":
                checkPermissionOrDeny(player, "yudxxterritory.command.delete", () -> handleDelete(player));
                break;
            case "middle":
                checkPermissionOrDeny(player, "yudxxterritory.command.middle", () -> handleMiddle(player));
                break;
            case "kick":
                checkPermissionOrDeny(player, "yudxxterritory.command.kick", () -> handleKick(player, args));
                break;
            case "settings":
                checkPermissionOrDeny(player, "yudxxterritory.command.settings", () -> handleSettings(player));
                break;
            case "list":
                checkPermissionOrDeny(player, "yudxxterritory.command.list", () -> handleList(player));
                break;
            case "gui":
                checkPermissionOrDeny(player, "yudxxterritory.command.gui", () -> handleGui(player));
                break;
            case "ban":
                checkPermissionOrDeny(player, "yudxxterritory.command.ban", () -> handleBan(player, args));
                break;
            case "unban":
                checkPermissionOrDeny(player, "yudxxterritory.command.unban", () -> handleUnban(player, args));
                break;
            default:
                sendHelp(player, 1);
                break;
        }
        return true;
    }

    private void checkPermissionOrDeny(Player player, String permission, Runnable action) {
        if (player.hasPermission(permission)) {
            action.run();
        } else {
            messageManager.send(player, "no-permission");
        }
    }

    private void sendHelp(Player player, int page) {
        int totalPages = (int) Math.ceil((double) HELP_ENTRIES.size() / HELP_PER_PAGE);
        if (page < 1 || page > totalPages) {
            messageManager.send(player, "invalid-page");
            return;
        }
        String version = plugin.getDescription().getVersion();
        player.sendMessage(HexColorUtil.color("&#AAAAAA&m------------------------------------------------"));
        player.sendMessage(HexColorUtil.color("&#55FFFF&lYudxxTerritory &#AAAAAAv" + version + " &#AAAAAA- Command Help &#AAAAAA(" + page + "/" + totalPages + ")"));
        player.sendMessage(HexColorUtil.color(""));
        int start = (page - 1) * HELP_PER_PAGE;
        int end = Math.min(start + HELP_PER_PAGE, HELP_ENTRIES.size());
        for (int i = start; i < end; i++) {
            String[] entry = HELP_ENTRIES.get(i);
            player.sendMessage(HexColorUtil.color("&#FFAA00" + entry[1] + " &#AAAAAA- " + entry[2]));
        }
        player.sendMessage(HexColorUtil.color(""));
        if (page < totalPages) {
            player.sendMessage(HexColorUtil.color("&#AAAAAAUse &#FFAA00/territory help " + (page + 1) + " &#AAAAAAfor next page"));
        }
        player.sendMessage(HexColorUtil.color("&#AAAAAA&m------------------------------------------------"));
    }

    private void handleAuto(Player player) {
        if (!player.hasPermission("yudxxterritory.world.teleport")) {
            messageManager.send(player, "no-permission");
            return;
        }
        if (!worldManager.isPlotWorld(player.getWorld().getName())) {
            messageManager.send(player, "not-in-plotworld");
            worldManager.teleportToPlotSpawn(player);
            messageManager.send(player, "teleported-to-plotworld");
            return;
        }
        if (configManager.isEconomyEnabled() && configManager.getAutoClaimCost() > 0) {
            messageManager.send(player, "auto-claim-cost", configManager.getAutoClaimCost());
            return;
        }
        boolean success = plotManager.autoClaim(player);
        if (success) {
            PlotData plot = plotManager.getPlayerPlotInWorld(player.getUniqueId(), player.getWorld().getName());
            if (plot == null) {
                List<PlotData> allPlots = plotManager.getPlayerPlots(player.getUniqueId());
                if (!allPlots.isEmpty()) {
                    plot = allPlots.get(allPlots.size() - 1);
                }
            }
            if (plot != null) {
                Location spawn = plotManager.getPlotSpawn(plot.getWorldName(), plot.getPlotX(), plot.getPlotZ());
                if (spawn != null) {
                    if (!player.getWorld().getName().equals(plot.getWorldName())) {
                        player.teleport(worldManager.getPlotWorldSpawn(plot.getWorldName()));
                        PlotData finalPlot = plot;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                player.teleport(plotManager.getPlotSpawn(finalPlot.getWorldName(), finalPlot.getPlotX(), finalPlot.getPlotZ()));
                            }
                        }, 5L);
                    } else {
                        player.teleport(spawn);
                    }
                }
                messageManager.send(player, "plot-claim-success", plot.getPlotX(), plot.getPlotZ());
                soundManager.playSound(player, "plot-claim");
            }
        } else {
            messageManager.send(player, "claim-failed");
        }
    }

    private void handleClaim(Player player) {
        if (!worldManager.isPlotWorld(player.getWorld().getName())) {
            messageManager.send(player, "not-in-plotworld");
            return;
        }
        PlotData currentPlot = plotManager.getPlotAtLocation(player.getLocation());
        if (currentPlot == null) {
            messageManager.send(player, "not-on-plot");
            return;
        }
        if (currentPlot.hasOwner()) {
            messageManager.send(player, "plot-already-claimed");
            return;
        }
        boolean success = plotManager.claimPlot(player, currentPlot.getWorldName(), currentPlot.getPlotX(), currentPlot.getPlotZ());
        if (success) {
            Location spawn = plotManager.getPlotSpawn(currentPlot.getWorldName(), currentPlot.getPlotX(), currentPlot.getPlotZ());
            if (spawn != null) {
                player.teleport(spawn);
            }
            messageManager.send(player, "plot-claim-success", currentPlot.getPlotX(), currentPlot.getPlotZ());
            soundManager.playSound(player, "plot-claim");
        } else {
            messageManager.send(player, "claim-failed");
        }
    }

    private void handleHome(Player player, String[] args) {
        if (!player.hasPermission("yudxxterritory.plot.teleport")) {
            messageManager.send(player, "no-permission");
            return;
        }
        List<PlotData> plots = plotManager.getPlayerPlots(player.getUniqueId());
        if (plots.isEmpty()) {
            messageManager.send(player, "no-plots");
            return;
        }
        int index = 0;
        if (args.length >= 2) {
            try {
                index = Integer.parseInt(args[1]) - 1;
            } catch (NumberFormatException e) {
                messageManager.send(player, "invalid-number");
                return;
            }
        }
        if (index < 0 || index >= plots.size()) {
            messageManager.send(player, "invalid-plot-index");
            return;
        }
        PlotData plot = plots.get(index);
        World plotWorld = Bukkit.getWorld(plot.getWorldName());
        if (plotWorld == null) {
            messageManager.send(player, "world-not-loaded");
            return;
        }
        Location spawn = plotManager.getPlotSpawn(plot.getWorldName(), plot.getPlotX(), plot.getPlotZ());
        if (spawn == null) {
            messageManager.send(player, "world-not-loaded");
            return;
        }
        int finalIndex = index;
        if (!player.getWorld().getName().equals(plot.getWorldName())) {
            Location worldSpawn = worldManager.getPlotWorldSpawn(plot.getWorldName());
            if (worldSpawn != null) {
                player.teleport(worldSpawn);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.teleport(spawn);
                        messageManager.send(player, "plot-home-success", finalIndex + 1, plot.getPlotX(), plot.getPlotZ());
                        soundManager.playSound(player, "plot-teleport");
                    }
                }, 5L);
                return;
            }
        }
        player.teleport(spawn);
        messageManager.send(player, "plot-home-success", finalIndex + 1, plot.getPlotX(), plot.getPlotZ());
        soundManager.playSound(player, "plot-teleport");
    }

    private void handleVisit(Player player, String[] args) {
        if (!player.hasPermission("yudxxterritory.plot.teleport.other")) {
            messageManager.send(player, "no-permission");
            return;
        }
        if (args.length < 2) {
            messageManager.send(player, "usage-visit");
            return;
        }
        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messageManager.send(player, "target-no-plots");
            return;
        }
        List<PlotData> plots = plotManager.getPlayerPlots(target.getUniqueId());
        if (plots.isEmpty()) {
            messageManager.send(player, "target-no-plots");
            return;
        }
        Random random = new Random();
        PlotData plot = plots.get(random.nextInt(plots.size()));
        World plotWorld = Bukkit.getWorld(plot.getWorldName());
        if (plotWorld == null) {
            messageManager.send(player, "world-not-loaded");
            return;
        }
        Location spawn = plotManager.getPlotSpawn(plot.getWorldName(), plot.getPlotX(), plot.getPlotZ());
        if (spawn == null) {
            messageManager.send(player, "world-not-loaded");
            return;
        }
        String ownerName = target.getName() != null ? target.getName() : "???";
        if (!player.getWorld().getName().equals(plot.getWorldName())) {
            Location worldSpawn = worldManager.getPlotWorldSpawn(plot.getWorldName());
            if (worldSpawn != null) {
                player.teleport(worldSpawn);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.teleport(spawn);
                        messageManager.send(player, "plot-visit-success", ownerName);
                        soundManager.playSound(player, "plot-teleport");
                    }
                }, 5L);
                return;
            }
        }
        player.teleport(spawn);
        messageManager.send(player, "plot-visit-success", ownerName);
        soundManager.playSound(player, "plot-teleport");
    }

    private void handleInfo(Player player) {
        if (!worldManager.isPlotWorld(player.getWorld().getName())) {
            messageManager.send(player, "not-in-plotworld");
            return;
        }
        PlotData plot = plotManager.getPlotAtLocation(player.getLocation());
        if (plot == null) {
            messageManager.send(player, "on-road");
            return;
        }
        player.sendMessage(HexColorUtil.color("&#AAAAAA&m----------------------------------------"));
        player.sendMessage(HexColorUtil.color("&#55FFFF&lTerritory Info"));
        player.sendMessage(HexColorUtil.color("&#AAAAAAWorld: &#FFFFFF" + plot.getWorldName()));
        player.sendMessage(HexColorUtil.color("&#AAAAAACoords: &#FFFFFF" + plot.getPlotX() + ", " + plot.getPlotZ()));
        if (plot.getName() != null) {
            player.sendMessage(HexColorUtil.color("&#AAAAAAName: &#FFAA00" + plot.getName()));
        }
        if (plot.hasOwner()) {
            String ownerName = Bukkit.getOfflinePlayer(plot.getOwner()).getName();
            player.sendMessage(HexColorUtil.color("&#AAAAAAOwner: &#FFFFFF" + (ownerName != null ? ownerName : "Unknown")));
        } else {
            player.sendMessage(HexColorUtil.color("&#AAAAAAStatus: &#55FF55Unclaimed"));
        }
        player.sendMessage(HexColorUtil.color("&#AAAAAAMerged: &#FFFFFF" + (plot.isMerged() ? "Yes" : "No")));
        player.sendMessage(HexColorUtil.color("&#AAAAAATrusted: &#FFFFFF" + plot.getTrusted().size()));
        player.sendMessage(HexColorUtil.color("&#AAAAAA&m----------------------------------------"));
    }

    private void handleSetName(Player player, String[] args) {
        if (args.length < 2) {
            messageManager.send(player, "usage-setname");
            return;
        }
        PlotData plot = plotManager.getPlotAtLocation(player.getLocation());
        if (plot == null || !plot.isOwner(player.getUniqueId())) {
            messageManager.send(player, "not-owner");
            return;
        }
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        if (name.length() > 32) {
            name = name.substring(0, 32);
        }
        plotManager.setPlotName(plot, name);
        messageManager.send(player, "setname-success", name);
        soundManager.playSound(player, "plot-named");
    }

    private void handleTrust(Player player, String[] args) {
        if (args.length < 2) {
            messageManager.send(player, "usage-trust");
            return;
        }
        PlotData plot = plotManager.getPlotAtLocation(player.getLocation());
        if (plot == null || !plot.isOwner(player.getUniqueId())) {
            messageManager.send(player, "not-owner");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messageManager.send(player, "player-not-found");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            messageManager.send(player, "cant-trust-self");
            return;
        }
        if (plot.isTrusted(target.getUniqueId())) {
            messageManager.send(player, "already-trusted");
            return;
        }
        plotManager.trustPlayer(plot, target.getUniqueId());
        messageManager.send(player, "trust-added", target.getName() != null ? target.getName() : "???");
        soundManager.playSound(player, "trust-add");
    }

    private void handleUntrust(Player player, String[] args) {
        if (args.length < 2) {
            messageManager.send(player, "usage-untrust");
            return;
        }
        PlotData plot = plotManager.getPlotAtLocation(player.getLocation());
        if (plot == null || !plot.isOwner(player.getUniqueId())) {
            messageManager.send(player, "not-owner");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!plot.isTrusted(target.getUniqueId())) {
            messageManager.send(player, "not-trusted");
            return;
        }
        plotManager.untrustPlayer(plot, target.getUniqueId());
        messageManager.send(player, "trust-removed", target.getName() != null ? target.getName() : "???");
        soundManager.playSound(player, "trust-remove");
        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null && plotManager.getPlotAtLocation(onlineTarget.getLocation()) == plot) {
                Location spawn = plotManager.getPlotRoadCenter(plot.getWorldName(), plot.getPlotX(), plot.getPlotZ());
                if (spawn != null) {
                    onlineTarget.teleport(spawn);
                }
            }
        }
    }

    private void handleMerge(Player player) {
        PlotData currentPlot = plotManager.getPlotAtLocation(player.getLocation());
        if (currentPlot == null || !currentPlot.isOwner(player.getUniqueId())) {
            messageManager.send(player, "not-owner");
            return;
        }
        if (currentPlot.isMerged()) {
            messageManager.send(player, "already-merged");
            return;
        }
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : directions) {
            int nx = currentPlot.getPlotX() + dir[0];
            int nz = currentPlot.getPlotZ() + dir[1];
            PlotData neighbor = plotManager.getPlot(currentPlot.getWorldName(), nx, nz);
            if (neighbor != null && neighbor.isOwner(player.getUniqueId()) && !neighbor.isMerged()) {
                plotManager.mergePlots(currentPlot, neighbor);
                messageManager.send(player, "merge-success", currentPlot.getPlotX(), currentPlot.getPlotZ(), nx, nz);
                soundManager.playSound(player, "plot-merge");
                return;
            }
        }
        messageManager.send(player, "no-adjacent-plot");
    }

    private void handleUnmerge(Player player) {
        PlotData currentPlot = plotManager.getPlotAtLocation(player.getLocation());
        if (currentPlot == null || !currentPlot.isOwner(player.getUniqueId())) {
            messageManager.send(player, "not-owner");
            return;
        }
        if (!currentPlot.isMerged()) {
            messageManager.send(player, "not-merged");
            return;
        }
        plotManager.unmergePlot(currentPlot);
        messageManager.send(player, "unmerge-success");
        soundManager.playSound(player, "plot-unmerge");
    }

    private void handleClear(Player player) {
        PlotData currentPlot = plotManager.getPlotAtLocation(player.getLocation());
        if (currentPlot == null || !currentPlot.isOwner(player.getUniqueId())) {
            messageManager.send(player, "not-owner");
            return;
        }
        clearManager.clearPlot(currentPlot, player);
        soundManager.playSound(player, "plot-clear");
    }

    private void handleUndo(Player player) {
        PlotData currentPlot = plotManager.getPlotAtLocation(player.getLocation());
        if (currentPlot == null || !currentPlot.isOwner(player.getUniqueId())) {
            messageManager.send(player, "not-owner");
            return;
        }
        clearManager.undoClear(currentPlot, player);
    }

    private void handleDelete(Player player) {
        PlotData currentPlot = plotManager.getPlotAtLocation(player.getLocation());
        if (currentPlot == null || !currentPlot.isOwner(player.getUniqueId())) {
            messageManager.send(player, "not-owner");
            return;
        }
        if (currentPlot.isMerged()) {
            plotManager.unmergePlot(currentPlot);
        }
        Location spawn = plotManager.getPlotRoadCenter(currentPlot.getWorldName(), currentPlot.getPlotX(), currentPlot.getPlotZ());
        clearManager.deletePlot(currentPlot, player);
        plotManager.deletePlotData(currentPlot);
        if (spawn != null) {
            player.teleport(spawn);
        }
        messageManager.send(player, "plot-delete-success");
        soundManager.playSound(player, "plot-delete");
    }

    private void handleMiddle(Player player) {
        PlotData currentPlot = plotManager.getPlotAtLocation(player.getLocation());
        if (currentPlot == null) {
            messageManager.send(player, "not-on-plot");
            return;
        }
        if (!currentPlot.isOwner(player.getUniqueId()) && !currentPlot.isTrusted(player.getUniqueId())) {
            messageManager.send(player, "no-permission-teleport");
            return;
        }
        Location center = plotManager.getPlotCenter(currentPlot.getWorldName(), currentPlot.getPlotX(), currentPlot.getPlotZ());
        if (center == null) {
            messageManager.send(player, "world-not-loaded");
            return;
        }
        player.teleport(center);
        messageManager.send(player, "plot-middle-success");
        soundManager.playSound(player, "plot-teleport");
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            messageManager.send(player, "usage-kick");
            return;
        }
        PlotData currentPlot = plotManager.getPlotAtLocation(player.getLocation());
        if (currentPlot == null || !currentPlot.isOwner(player.getUniqueId())) {
            messageManager.send(player, "not-owner");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageManager.send(player, "player-not-online");
            return;
        }
        PlotData targetPlot = plotManager.getPlotAtLocation(target.getLocation());
        if (targetPlot != currentPlot) {
            messageManager.send(player, "player-not-on-plot");
            return;
        }
        Location spawn = plotManager.getPlotRoadCenter(currentPlot.getWorldName(), currentPlot.getPlotX(), currentPlot.getPlotZ());
        if (spawn != null) {
            target.teleport(spawn);
        }
        messageManager.send(target, "kicked-from-plot", player.getName());
        messageManager.send(player, "kicked-player", target.getName());
        soundManager.playSound(player, "kick");
    }

    private void handleBan(Player player, String[] args) {
        if (args.length < 2) {
            messageManager.send(player, "usage-ban");
            return;
        }
        PlotData currentPlot = plotManager.getPlotAtLocation(player.getLocation());
        if (currentPlot == null || !currentPlot.isOwner(player.getUniqueId())) {
            messageManager.send(player, "not-owner");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messageManager.send(player, "player-not-found");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            messageManager.send(player, "cant-ban-self");
            return;
        }
        if (currentPlot.isBanned(target.getUniqueId())) {
            messageManager.send(player, "already-banned");
            return;
        }
        plotManager.banPlayer(currentPlot, target.getUniqueId());
        messageManager.send(player, "player-banned", target.getName() != null ? target.getName() : "???");
        soundManager.playSound(player, "ban");
        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null && plotManager.getPlotAtLocation(onlineTarget.getLocation()) == currentPlot) {
                List<PlotData> playerPlots = plotManager.getPlayerPlots(onlineTarget.getUniqueId());
                if (!playerPlots.isEmpty()) {
                    Random random = new Random();
                    PlotData randomPlot = playerPlots.get(random.nextInt(playerPlots.size()));
                    Location spawn = plotManager.getPlotSpawn(randomPlot.getWorldName(), randomPlot.getPlotX(), randomPlot.getPlotZ());
                    if (spawn != null) {
                        onlineTarget.teleport(spawn);
                    }
                }
                messageManager.send(onlineTarget, "banned-from-plot", player.getName());
            }
        }
    }

    private void handleUnban(Player player, String[] args) {
        if (args.length < 2) {
            messageManager.send(player, "usage-unban");
            return;
        }
        PlotData currentPlot = plotManager.getPlotAtLocation(player.getLocation());
        if (currentPlot == null || !currentPlot.isOwner(player.getUniqueId())) {
            messageManager.send(player, "not-owner");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!currentPlot.isBanned(target.getUniqueId())) {
            messageManager.send(player, "not-banned");
            return;
        }
        plotManager.unbanPlayer(currentPlot, target.getUniqueId());
        messageManager.send(player, "player-unbanned", target.getName() != null ? target.getName() : "???");
        soundManager.playSound(player, "unban");
    }

    private void handleSettings(Player player) {
        if (!player.hasPermission("yudxxterritory.plot.settings")) {
            messageManager.send(player, "no-permission");
            return;
        }
        PlotData currentPlot = plotManager.getPlotAtLocation(player.getLocation());
        if (currentPlot == null || !currentPlot.isOwner(player.getUniqueId())) {
            messageManager.send(player, "not-owner");
            return;
        }
        plotMenu.getSettingsMenu().openSettings(player, currentPlot);
        soundManager.playSound(player, "menu-open");
    }

    private void handleGui(Player player) {
        mainMenu.openMainMenu(player);
        soundManager.playSound(player, "menu-open");
    }

    private void handleList(Player player) {
        List<PlotData> plots = plotManager.getPlayerPlots(player.getUniqueId());
        if (plots.isEmpty()) {
            messageManager.send(player, "no-plots");
            return;
        }
        player.sendMessage(HexColorUtil.color("&#AAAAAA&m----------------------------------------"));
        player.sendMessage(HexColorUtil.color("&#55FFFF&lYour Territories"));
        int i = 1;
        for (PlotData plot : plots) {
            player.sendMessage(HexColorUtil.color("&#FFAA00#" + i + " &#AAAAAA- &#FFFFFF" + plot.getWorldName() + " &#AAAAAA(" + plot.getPlotX() + ", " + plot.getPlotZ() + ")"));
            i++;
        }
        player.sendMessage(HexColorUtil.color("&#AAAAAA&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("help", "auto", "claim", "home", "visit", "info", "setname", "trust", "untrust",
                "merge", "unmerge", "clear", "undo", "delete", "middle", "kick", "settings", "list", "gui", "ban", "unban");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("visit") || sub.equals("trust") || sub.equals("untrust") || sub.equals("kick") || sub.equals("ban") || sub.equals("unban")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}