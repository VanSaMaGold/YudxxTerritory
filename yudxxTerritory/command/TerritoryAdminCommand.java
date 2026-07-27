package yudxx.minecraft.spigot.yudxxTerritory.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import yudxx.minecraft.spigot.yudxxTerritory.YudxxTerritory;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotData;
import yudxx.minecraft.spigot.yudxxTerritory.manager.ConfigManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.PlotManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.SoundManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.WorldManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.MessageManager;
import yudxx.minecraft.spigot.yudxxTerritory.util.HexColorUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TerritoryAdminCommand implements CommandExecutor, TabCompleter {

    private final YudxxTerritory plugin;
    private final PlotManager plotManager;
    private final WorldManager worldManager;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;

    private static final int HELP_PER_PAGE = 5;

    private static final List<String[]> ADMIN_HELP_ENTRIES = Arrays.asList(
        new String[]{"createworld", "/territoryadmin createworld <world>", "Create a territory world"},
        new String[]{"generateworld", "/territoryadmin generateworld <world>", "Generate territory world terrain"},
        new String[]{"deleteworld", "/territoryadmin deleteworld <world>", "Delete a territory world"},
        new String[]{"setborder", "/territoryadmin setborder <world> <material>", "Set border material for a world"},
        new String[]{"reload", "/territoryadmin reload", "Reload configuration"},
        new String[]{"info", "/territoryadmin info <world> <x> <z>", "View territory info"},
        new String[]{"delete", "/territoryadmin delete <world> <x> <z>", "Delete a specific territory"},
        new String[]{"clear", "/territoryadmin clear <world> <x> <z>", "Clear a specific territory"},
        new String[]{"reset", "/territoryadmin reset <world> <x> <z>", "Reset a specific territory"},
        new String[]{"permissions", "/territoryadmin permissions [player]", "View permission list"}
    );

    private static final Map<String, String> PERMISSION_LIST = new LinkedHashMap<>();

    static {
        PERMISSION_LIST.put("yudxxterritory.admin", "Territory system admin - all admin commands");
        PERMISSION_LIST.put("yudxxterritory.admin.bypass", "Bypass territory protection rules");
        PERMISSION_LIST.put("yudxxterritory.command.auto", "Use /territory auto to auto-claim");
        PERMISSION_LIST.put("yudxxterritory.command.claim", "Use /territory claim to manual claim");
        PERMISSION_LIST.put("yudxxterritory.command.home", "Use /territory home to teleport");
        PERMISSION_LIST.put("yudxxterritory.command.visit", "Use /territory visit to visit others");
        PERMISSION_LIST.put("yudxxterritory.command.info", "Use /territory info to view info");
        PERMISSION_LIST.put("yudxxterritory.command.trust", "Use /territory trust to trust players");
        PERMISSION_LIST.put("yudxxterritory.command.untrust", "Use /territory untrust to remove trust");
        PERMISSION_LIST.put("yudxxterritory.command.merge", "Use /territory merge to merge territories");
        PERMISSION_LIST.put("yudxxterritory.command.unmerge", "Use /territory unmerge to cancel merge");
        PERMISSION_LIST.put("yudxxterritory.command.clear", "Use /territory clear to clear territory");
        PERMISSION_LIST.put("yudxxterritory.command.delete", "Use /territory delete to delete territory");
        PERMISSION_LIST.put("yudxxterritory.command.middle", "Use /territory middle to teleport to center");
        PERMISSION_LIST.put("yudxxterritory.command.kick", "Use /territory kick to kick players");
        PERMISSION_LIST.put("yudxxterritory.command.settings", "Use /territory settings to open settings");
        PERMISSION_LIST.put("yudxxterritory.command.list", "Use /territory list to view territory list");
        PERMISSION_LIST.put("yudxxterritory.command.gui", "Use /territory gui to open main menu");
        PERMISSION_LIST.put("yudxxterritory.plot.limit.1", "Max 1 territory");
        PERMISSION_LIST.put("yudxxterritory.plot.limit.3", "Max 3 territories");
        PERMISSION_LIST.put("yudxxterritory.plot.limit.5", "Max 5 territories");
        PERMISSION_LIST.put("yudxxterritory.plot.limit.10", "Max 10 territories");
        PERMISSION_LIST.put("yudxxterritory.plot.limit.unlimited", "Unlimited territories");
        PERMISSION_LIST.put("yudxxterritory.plot.settings", "Modify territory settings");
        PERMISSION_LIST.put("yudxxterritory.plot.teleport", "Teleport to own territory");
        PERMISSION_LIST.put("yudxxterritory.plot.teleport.other", "Teleport to other''s territory");
        PERMISSION_LIST.put("yudxxterritory.world.teleport", "Teleport to territory world");
    }

    public TerritoryAdminCommand(YudxxTerritory plugin, PlotManager plotManager, WorldManager worldManager,
                                 ConfigManager configManager, MessageManager messageManager,
                                 SoundManager soundManager) {
        this.plugin = plugin;
        this.plotManager = plotManager;
        this.worldManager = worldManager;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("yudxxterritory.admin")) {
            messageManager.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            sendAdminHelp(sender, 1);
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
                        messageManager.send(sender, "invalid-number");
                        return true;
                    }
                }
                sendAdminHelp(sender, page);
                break;
            case "createworld":
                handleCreateWorld(sender, args);
                break;
            case "generateworld":
                handleGenerateWorld(sender, args);
                break;
            case "deleteworld":
                handleDeleteWorld(sender, args);
                break;
            case "setborder":
                handleSetBorder(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "delete":
                handleDelete(sender, args);
                break;
            case "clear":
                handleClear(sender, args);
                break;
            case "reset":
                handleReset(sender, args);
                break;
            case "permissions":
                handlePermissions(sender, args);
                break;
            default:
                sendAdminHelp(sender, 1);
                break;
        }
        return true;
    }

    private void sendAdminHelp(CommandSender sender, int page) {
        int totalPages = (int) Math.ceil((double) ADMIN_HELP_ENTRIES.size() / HELP_PER_PAGE);
        if (page < 1 || page > totalPages) {
            messageManager.send(sender, "invalid-page");
            return;
        }
        String version = plugin.getDescription().getVersion();
        sender.sendMessage(HexColorUtil.color("&#AAAAAA&m------------------------------------------------"));
        sender.sendMessage(HexColorUtil.color("&#FF5555&lYudxxTerritory &#AAAAAAv" + version + " &#AAAAAA- Admin Commands &#AAAAAA(" + page + "/" + totalPages + ")"));
        sender.sendMessage(HexColorUtil.color(""));
        int start = (page - 1) * HELP_PER_PAGE;
        int end = Math.min(start + HELP_PER_PAGE, ADMIN_HELP_ENTRIES.size());
        for (int i = start; i < end; i++) {
            String[] entry = ADMIN_HELP_ENTRIES.get(i);
            sender.sendMessage(HexColorUtil.color("&#FFAA00" + entry[1] + " &#AAAAAA- " + entry[2]));
        }
        sender.sendMessage(HexColorUtil.color(""));
        if (page < totalPages) {
            sender.sendMessage(HexColorUtil.color("&#AAAAAAUse &#FFAA00/territoryadmin help " + (page + 1) + " &#AAAAAAfor next page"));
        }
        sender.sendMessage(HexColorUtil.color("&#AAAAAA&m------------------------------------------------"));
    }

    private void handlePermissions(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                messageManager.send(sender, "player-not-online");
                return;
            }
            sender.sendMessage(HexColorUtil.color("&#AAAAAA&m----------------------------------------"));
            sender.sendMessage(HexColorUtil.color("&#FFAA00&l" + target.getName() + " &#AAAAAA- Territory Permissions"));
            sender.sendMessage(HexColorUtil.color(""));
            for (Map.Entry<String, String> entry : PERMISSION_LIST.entrySet()) {
                boolean has = target.hasPermission(entry.getKey());
                String status = has ? "&#55FF55\u2714" : "&#FF5555\u2718";
                sender.sendMessage(HexColorUtil.color(status + " &#FFFFFF" + entry.getKey() + " &#AAAAAA- " + entry.getValue()));
            }
            sender.sendMessage(HexColorUtil.color("&#AAAAAA&m----------------------------------------"));
        } else {
            sender.sendMessage(HexColorUtil.color("&#AAAAAA&m----------------------------------------"));
            sender.sendMessage(HexColorUtil.color("&#FFAA00&lYudxxTerritory Permission Nodes"));
            sender.sendMessage(HexColorUtil.color(""));
            for (Map.Entry<String, String> entry : PERMISSION_LIST.entrySet()) {
                sender.sendMessage(HexColorUtil.color("&#FFFFFF" + entry.getKey() + " &#AAAAAA- " + entry.getValue()));
            }
            sender.sendMessage(HexColorUtil.color(""));
            sender.sendMessage(HexColorUtil.color("&#AAAAAAUse &#FFAA00/territoryadmin permissions <player> &#AAAAAAto check a player"));
            sender.sendMessage(HexColorUtil.color("&#AAAAAA&m----------------------------------------"));
        }
    }

    private void handleCreateWorld(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageManager.send(sender, "usage-createworld");
            return;
        }
        String worldName = args[1];
        worldManager.createPlotWorld(worldName);
        messageManager.send(sender, "world-created", worldName);
        if (sender instanceof Player) soundManager.playSound((Player) sender, "world-create");
    }

    private void handleGenerateWorld(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageManager.send(sender, "usage-generateworld");
            return;
        }
        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            messageManager.send(sender, "world-not-exist");
            return;
        }
        messageManager.send(sender, "generating-world");
        worldManager.generatePlotWorld(worldName);
        messageManager.send(sender, "world-generated", worldName);
        if (sender instanceof Player) soundManager.playSound((Player) sender, "world-generate");
    }

    private void handleDeleteWorld(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageManager.send(sender, "usage-deleteworld");
            return;
        }
        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            messageManager.send(sender, "world-not-exist");
            return;
        }
        Bukkit.unloadWorld(world, false);
        messageManager.send(sender, "world-deleted", worldName);
        if (sender instanceof Player) soundManager.playSound((Player) sender, "world-delete");
    }

    private void handleSetBorder(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messageManager.send(sender, "usage-setborder");
            return;
        }
        String worldName = args[1];
        if (!worldManager.isPlotWorld(worldName)) {
            messageManager.send(sender, "world-not-exist");
            return;
        }
        String materialName = args[2].toUpperCase();
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(HexColorUtil.color("&#FF5555Invalid material: " + materialName));
            return;
        }
        if (!material.isBlock()) {
            sender.sendMessage(HexColorUtil.color("&#FF5555Material must be a block"));
            return;
        }
        worldManager.replaceBorderMaterial(worldName, material, sender);
        if (sender instanceof Player) soundManager.playSound((Player) sender, "admin-action");
    }

    private void handleReload(CommandSender sender) {
        configManager.reloadConfig();
        messageManager.reloadMessages();
        messageManager.send(sender, "config-reloaded");
        if (sender instanceof Player) soundManager.playSound((Player) sender, "admin-action");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messageManager.send(sender, "usage-admin-info");
            return;
        }
        String worldName = args[1];
        int plotX, plotZ;
        try {
            plotX = Integer.parseInt(args[2]);
            plotZ = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            messageManager.send(sender, "invalid-coordinates");
            return;
        }
        PlotData plot = plotManager.getPlot(worldName, plotX, plotZ);
        if (plot == null) {
            messageManager.send(sender, "plot-not-exist");
            return;
        }
        sender.sendMessage(HexColorUtil.color("&#AAAAAA&m----------------------------------------"));
        sender.sendMessage(HexColorUtil.color("&#55FFFF&lTerritory Info"));
        sender.sendMessage(HexColorUtil.color("&#AAAAAAID: &#FFFFFF" + plot.getId()));
        sender.sendMessage(HexColorUtil.color("&#AAAAAAWorld: &#FFFFFF" + plot.getWorldName()));
        sender.sendMessage(HexColorUtil.color("&#AAAAAACoords: &#FFFFFF" + plot.getPlotX() + ", " + plot.getPlotZ()));
        if (plot.hasOwner()) {
            String ownerName = Bukkit.getOfflinePlayer(plot.getOwner()).getName();
            sender.sendMessage(HexColorUtil.color("&#AAAAAAOwner: &#FFFFFF" + (ownerName != null ? ownerName : "Unknown")));
        } else {
            sender.sendMessage(HexColorUtil.color("&#AAAAAAStatus: &#55FF55Unclaimed"));
        }
        sender.sendMessage(HexColorUtil.color("&#AAAAAAMerged: &#FFFFFF" + (plot.isMerged() ? "Yes" : "No")));
        sender.sendMessage(HexColorUtil.color("&#AAAAAATrusted: &#FFFFFF" + plot.getTrusted().size()));
        sender.sendMessage(HexColorUtil.color("&#AAAAAA&m----------------------------------------"));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messageManager.send(sender, "usage-admin-delete");
            return;
        }
        String worldName = args[1];
        int plotX, plotZ;
        try {
            plotX = Integer.parseInt(args[2]);
            plotZ = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            messageManager.send(sender, "invalid-coordinates");
            return;
        }
        PlotData plot = plotManager.getPlot(worldName, plotX, plotZ);
        if (plot == null) {
            messageManager.send(sender, "plot-not-exist");
            return;
        }
        if (plot.isMerged()) {
            plotManager.unmergePlot(plot);
        }
        worldManager.clearPlotInteriorProgressive(worldName, plotX, plotZ, sender);
        plotManager.deletePlotData(plot);
        messageManager.send(sender, "admin-plot-deleted");
        if (sender instanceof Player) soundManager.playSound((Player) sender, "admin-action");
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messageManager.send(sender, "usage-admin-clear");
            return;
        }
        String worldName = args[1];
        int plotX, plotZ;
        try {
            plotX = Integer.parseInt(args[2]);
            plotZ = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            messageManager.send(sender, "invalid-coordinates");
            return;
        }
        PlotData plot = plotManager.getPlot(worldName, plotX, plotZ);
        if (plot == null) {
            messageManager.send(sender, "plot-not-exist");
            return;
        }
        worldManager.clearPlotInteriorProgressive(worldName, plotX, plotZ, sender);
        plotManager.clearPlotData(plot);
        messageManager.send(sender, "admin-plot-cleared");
        if (sender instanceof Player) soundManager.playSound((Player) sender, "admin-action");
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messageManager.send(sender, "usage-admin-reset");
            return;
        }
        String worldName = args[1];
        int plotX, plotZ;
        try {
            plotX = Integer.parseInt(args[2]);
            plotZ = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            messageManager.send(sender, "invalid-coordinates");
            return;
        }
        PlotData plot = plotManager.getPlot(worldName, plotX, plotZ);
        if (plot == null) {
            messageManager.send(sender, "plot-not-exist");
            return;
        }
        if (plot.isMerged()) {
            plotManager.unmergePlot(plot);
        }
        worldManager.clearPlotInteriorProgressive(worldName, plotX, plotZ, sender);
        plotManager.clearPlotData(plot);
        plotManager.generatePlotBorder(worldName, plotX, plotZ);
        messageManager.send(sender, "admin-plot-reset");
        if (sender instanceof Player) soundManager.playSound((Player) sender, "admin-action");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("help", "createworld", "generateworld", "deleteworld", "setborder", "reload", "info", "delete", "clear", "reset", "permissions");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("permissions")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            if (sub.equals("generateworld") || sub.equals("deleteworld") || sub.equals("info") || sub.equals("delete") || sub.equals("clear") || sub.equals("reset")) {
                return configManager.getPlotWorlds().stream()
                    .filter(w -> w.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}