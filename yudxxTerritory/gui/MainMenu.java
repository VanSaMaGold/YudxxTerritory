package yudxx.minecraft.spigot.yudxxTerritory.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotData;
import yudxx.minecraft.spigot.yudxxTerritory.listener.MenuListener;
import yudxx.minecraft.spigot.yudxxTerritory.manager.ConfigManager;
import yudxx.minecraft.spigot.yudxxTerritory.manager.PlotManager;
import yudxx.minecraft.spigot.yudxxTerritory.util.HexColorUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class MainMenu {

    private final PlotManager plotManager;
    private final PlotMenu plotMenu;
    private final ConfigManager configManager;

    public MainMenu(PlotManager plotManager, PlotMenu plotMenu, ConfigManager configManager) {
        this.plotManager = plotManager;
        this.plotMenu = plotMenu;
        this.configManager = configManager;
    }

    public void openMainMenu(Player player) {
        FileConfiguration gui = configManager.getGuiConfig();
        List<PlotData> plots = plotManager.getPlayerPlots(player.getUniqueId());
        String title = gui.getString("main-menu.title", "&8&lTerritory System - Main Menu");
        Inventory inv = Bukkit.createInventory(null, 54, HexColorUtil.color(title));

        int slot = 0;
        for (PlotData plot : plots) {
            if (slot >= 45) break;
            String name = gui.getString("main-menu.territory-item.name", "&aTerritory #{0}");
            name = MessageFormat.format(name, slot + 1);
            List<String> lore = getLoreList(gui, "main-menu.territory-item.lore",
                plot.getWorldName(), plot.getPlotX(), plot.getPlotZ());
            inv.setItem(slot, createItem(Material.GRASS_BLOCK, name, lore));
            slot++;
        }

        if (plots.isEmpty()) {
            String name = gui.getString("main-menu.no-territory-item.name", "&cNo Territories");
            List<String> lore = getLoreList(gui, "main-menu.no-territory-item.lore");
            inv.setItem(22, createItem(Material.BARRIER, name, lore));
        }

        String autoName = gui.getString("main-menu.auto-claim-item.name", "&eAuto Claim Territory");
        List<String> autoLore = getLoreList(gui, "main-menu.auto-claim-item.lore");
        inv.setItem(49, createItem(Material.ENDER_PEARL, autoName, autoLore));

        player.openInventory(inv);
        MenuListener.registerMenu(player, "main");
    }

    private List<String> getLoreList(FileConfiguration gui, String path, Object... args) {
        List<String> raw = gui.getStringList(path);
        List<String> result = new ArrayList<>();
        for (String line : raw) {
            if (args.length > 0) {
                line = MessageFormat.format(line, args);
            }
            result.add(HexColorUtil.color(line));
        }
        return result;
    }

    public String getConfigTitle() {
        FileConfiguration gui = configManager.getGuiConfig();
        return gui.getString("main-menu.title", "&8&lTerritory System - Main Menu");
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
}