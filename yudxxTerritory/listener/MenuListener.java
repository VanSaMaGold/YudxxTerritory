package yudxx.minecraft.spigot.yudxxTerritory.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import yudxx.minecraft.spigot.yudxxTerritory.YudxxTerritory;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotData;
import yudxx.minecraft.spigot.yudxxTerritory.gui.MainMenu;
import yudxx.minecraft.spigot.yudxxTerritory.gui.PlotMenu;
import yudxx.minecraft.spigot.yudxxTerritory.gui.SettingsMenu;
import yudxx.minecraft.spigot.yudxxTerritory.manager.ClearManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.ConfigManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.PlotManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.MessageManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.SoundManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.WorldManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MenuListener implements Listener {

    private static final Map<UUID, String> menuCache = new ConcurrentHashMap<>();

    private final YudxxTerritory plugin;
    private final PlotManager plotManager;
    private final PlotMenu plotMenu;
    private final MainMenu mainMenu;
    private final SettingsMenu settingsMenu;
    private final MessageManager messageManager;
    private final WorldManager worldManager;
    private final SoundManager soundManager;
    private final ClearManager clearManager;

    public MenuListener(YudxxTerritory plugin, PlotManager plotManager, PlotMenu plotMenu, MainMenu mainMenu,
                        SettingsMenu settingsMenu, MessageManager messageManager, WorldManager worldManager,
                        SoundManager soundManager, ClearManager clearManager) {
        this.plugin = plugin;
        this.plotManager = plotManager;
        this.plotMenu = plotMenu;
        this.mainMenu = mainMenu;
        this.settingsMenu = settingsMenu;
        this.messageManager = messageManager;
        this.worldManager = worldManager;
        this.soundManager = soundManager;
        this.clearManager = clearManager;
    }

    public static void registerMenu(Player player, String menuType) {
        menuCache.put(player.getUniqueId(), menuType);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        menuCache.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String menuType = menuCache.get(player.getUniqueId());
        if (menuType == null) return;

        event.setCancelled(true);

        soundManager.playSound(player, "menu-click");

        switch (menuType) {
            case "main":
                handleMainMenu(player, event.getSlot());
                break;
            case "plot":
                handlePlotMenu(player, event.getSlot());
                break;
            case "trust":
                handleTrustMenu(player, event.getSlot());
                break;
            case "settings":
                handleSettingsMenu(player, event.getSlot());
                break;
        }
    }

    private void handleMainMenu(Player player, int slot) {
        if (slot == 49) {
            player.closeInventory();
            player.performCommand("territory auto");
            return;
        }
        var plots = plotManager.getPlayerPlots(player.getUniqueId());
        if (slot >= 0 && slot < plots.size()) {
            PlotData plot = plots.get(slot);
            player.closeInventory();
            plotMenu.openPlotMenu(player, plot);
        }
    }

    private void handlePlotMenu(Player player, int slot) {
        PlotData plot = plotManager.getPlotAtLocation(player.getLocation());
        if (plot == null || !plot.isOwner(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        switch (slot) {
            case 11:
                Location center = plotManager.getPlotCenter(plot.getWorldName(), plot.getPlotX(), plot.getPlotZ());
                if (center != null) {
                    player.teleport(center);
                    messageManager.send(player, "plot-middle-success");
                }
                player.closeInventory();
                break;
            case 12:
                plotMenu.openTrustMenu(player, plot);
                break;
            case 13:
                Location spawn = plotManager.getPlotSpawn(plot.getWorldName(), plot.getPlotX(), plot.getPlotZ());
                if (spawn != null) {
                    player.teleport(spawn);
                    messageManager.send(player, "plot-home-success", 1, plot.getPlotX(), plot.getPlotZ());
                }
                player.closeInventory();
                break;
            case 14:
                settingsMenu.openSettings(player, plot);
                break;
            case 15:
                if (plot.isMerged()) plotManager.unmergePlot(plot);
                Location road = plotManager.getPlotRoadCenter(plot.getWorldName(), plot.getPlotX(), plot.getPlotZ());
                clearManager.deletePlot(plot, player);
                plotManager.deletePlotData(plot);
                if (road != null) player.teleport(road);
                player.closeInventory();
                messageManager.send(player, "plot-delete-success");
                break;
            case 16:
                clearManager.clearPlot(plot, player);
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    private void handleTrustMenu(Player player, int slot) {
        PlotData plot = plotManager.getPlotAtLocation(player.getLocation());
        if (plot == null || !plot.isOwner(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        if (slot == 53) {
            plotMenu.openPlotMenu(player, plot);
            return;
        }
        if (slot >= 0 && slot < 45) {
            ItemStack clicked = player.getOpenInventory().getItem(slot);
            if (clicked != null && clicked.getItemMeta() instanceof SkullMeta) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                if (meta.getOwningPlayer() != null) {
                    UUID targetUuid = meta.getOwningPlayer().getUniqueId();
                    if (plot.isTrusted(targetUuid)) {
                        plotManager.untrustPlayer(plot, targetUuid);
                        String name = meta.getOwningPlayer().getName();
                        messageManager.send(player, "trust-removed", name != null ? name : "???");
                        plotMenu.openTrustMenu(player, plot);
                    }
                }
            }
        }
    }

    private void handleSettingsMenu(Player player, int slot) {
        PlotData plot = plotManager.getPlotAtLocation(player.getLocation());
        if (plot == null || !plot.isOwner(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        if (slot == 49) {
            plotMenu.openPlotMenu(player, plot);
            return;
        }
        settingsMenu.handleSettingClick(player, plot, slot);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (!worldManager.isPlotWorld(event.getPlayer().getWorld().getName())) return;
        PlotData plot = plotManager.getPlotAtLocation(event.getClickedBlock().getLocation());
        if (plot == null) return;
        Material type = event.getClickedBlock().getType();
        ConfigManager.WorldSettings ws = plugin.getConfigManager().getWorldSettings(event.getPlayer().getWorld().getName());
        Material borderMat = ws.getBorderMaterial();
        if (type == borderMat) {
            if (plot.isOwner(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
                soundManager.playSound(event.getPlayer(), "menu-open");
                plotMenu.openPlotMenu(event.getPlayer(), plot);
            }
        }
    }
}