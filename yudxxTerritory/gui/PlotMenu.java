package yudxx.minecraft.spigot.yudxxTerritory.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import yudxx.minecraft.spigot.yudxxTerritory.data.PlotData;
import yudxx.minecraft.spigot.yudxxTerritory.listener.MenuListener;
import yudxx.minecraft.spigot.yudxxTerritory.manager.ConfigManager;
import yudxx.minecraft.spigot.yudxxTerritory.util.HexColorUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlotMenu {

    private final SettingsMenu settingsMenu;
    private final ConfigManager configManager;

    public PlotMenu(SettingsMenu settingsMenu, ConfigManager configManager) {
        this.settingsMenu = settingsMenu;
        this.configManager = configManager;
    }

    public void openPlotMenu(Player player, PlotData plot) {
        FileConfiguration gui = configManager.getGuiConfig();
        String title = MessageFormat.format(
            gui.getString("plot-menu.title", "&8Territory Management - &b{0}, {1}"),
            plot.getPlotX(), plot.getPlotZ()
        );
        Inventory inv = Bukkit.createInventory(null, 27, HexColorUtil.color(title));

        String mergedDisplay = plot.isMerged()
            ? gui.getString("display.merged-true", "&aYes")
            : gui.getString("display.merged-false", "&cNo");

        String infoName = gui.getString("plot-menu.info-item.name", "&aTerritory Info");
        List<String> infoLore = formatLore(gui.getStringList("plot-menu.info-item.lore"),
            plot.getWorldName(), plot.getPlotX(), plot.getPlotZ(), mergedDisplay, plot.getTrusted().size());
        inv.setItem(10, createItem(Material.GRASS_BLOCK, infoName, infoLore));

        String centerName = gui.getString("plot-menu.center-item.name", "&eTeleport to Center");
        List<String> centerLore = colorLore(gui.getStringList("plot-menu.center-item.lore"));
        inv.setItem(11, createItem(Material.ENDER_PEARL, centerName, centerLore));

        String trustName = gui.getString("plot-menu.trust-item.name", "&bTrust Management");
        List<String> trustLore = formatLore(gui.getStringList("plot-menu.trust-item.lore"), plot.getTrusted().size());
        inv.setItem(12, createItem(Material.PLAYER_HEAD, trustName, trustLore));

        String homeName = gui.getString("plot-menu.home-item.name", "&eTeleport to Territory");
        List<String> homeLore = colorLore(gui.getStringList("plot-menu.home-item.lore"));
        inv.setItem(13, createItem(Material.OAK_DOOR, homeName, homeLore));

        String settingsName = gui.getString("plot-menu.settings-item.name", "&dTerritory Settings");
        List<String> settingsLore = colorLore(gui.getStringList("plot-menu.settings-item.lore"));
        inv.setItem(14, createItem(Material.COMPARATOR, settingsName, settingsLore));

        String deleteName = gui.getString("plot-menu.delete-item.name", "&cDelete Territory");
        List<String> deleteLore = colorLore(gui.getStringList("plot-menu.delete-item.lore"));
        inv.setItem(15, createItem(Material.BARRIER, deleteName, deleteLore));

        String clearName = gui.getString("plot-menu.clear-item.name", "&eClear Territory");
        List<String> clearLore = colorLore(gui.getStringList("plot-menu.clear-item.lore"));
        inv.setItem(16, createItem(Material.WATER_BUCKET, clearName, clearLore));

        player.openInventory(inv);
        MenuListener.registerMenu(player, "plot");
    }

    public void openTrustMenu(Player player, PlotData plot) {
        FileConfiguration gui = configManager.getGuiConfig();
        String title = MessageFormat.format(
            gui.getString("trust-menu.title", "&8Trust Management - &b{0}, {1}"),
            plot.getPlotX(), plot.getPlotZ()
        );
        Inventory inv = Bukkit.createInventory(null, 54, HexColorUtil.color(title));

        List<UUID> trusted = plot.getTrusted();
        int slot = 0;
        for (UUID uuid : trusted) {
            if (slot >= 45) break;
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) name = uuid.toString().substring(0, 8);
            inv.setItem(slot, createSkullItem(name, "&e" + name,
                "&7" + "Click to remove trust"
            ));
            slot++;
        }

        String addName = gui.getString("trust-menu.add-item.name", "&aAdd Trusted Player");
        List<String> addLore = colorLore(gui.getStringList("trust-menu.add-item.lore"));
        inv.setItem(49, createItem(Material.ARROW, addName, addLore));

        String backName = gui.getString("trust-menu.back-item.name", "&cBack");
        List<String> backLore = colorLore(gui.getStringList("trust-menu.back-item.lore"));
        inv.setItem(53, createItem(Material.BARRIER, backName, backLore));

        player.openInventory(inv);
        MenuListener.registerMenu(player, "trust");
    }

    public SettingsMenu getSettingsMenu() {
        return settingsMenu;
    }

    public String getConfigPlotMenuTitle() {
        FileConfiguration gui = configManager.getGuiConfig();
        return gui.getString("plot-menu.title", "&8Territory Management");
    }

    public String getConfigTrustMenuTitle() {
        FileConfiguration gui = configManager.getGuiConfig();
        return gui.getString("trust-menu.title", "&8Trust Management");
    }

    private List<String> formatLore(List<String> raw, Object... args) {
        List<String> result = new ArrayList<>();
        for (String line : raw) {
            result.add(HexColorUtil.color(MessageFormat.format(line, args)));
        }
        return result;
    }

    private List<String> colorLore(List<String> raw) {
        List<String> result = new ArrayList<>();
        for (String line : raw) {
            result.add(HexColorUtil.color(line));
        }
        return result;
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

    private ItemStack createSkullItem(String owner, String name, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            meta.setDisplayName(HexColorUtil.color(name));
            List<String> loreList = new ArrayList<>();
            for (String l : lore) {
                loreList.add(HexColorUtil.color(l));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}