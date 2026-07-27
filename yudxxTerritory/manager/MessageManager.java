package yudxx.minecraft.spigot.yudxxTerritory.manager;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import yudxx.minecraft.spigot.yudxxTerritory.util.HexColorUtil;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

public class MessageManager {

    private final JavaPlugin plugin;
    private FileConfiguration messages;
    private File messagesFile;
    private String prefix;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = messages.getString("prefix", "&7[&b领地&7]&f ");
    }

    public void reloadMessages() {
        loadMessages();
    }

    public String getPrefix() {
        return HexColorUtil.color(prefix);
    }

    public String getMessage(String key) {
        String msg = messages.getString(key, "&cMissing message: " + key);
        return HexColorUtil.color(msg);
    }

    public String getMessage(String key, Object... args) {
        String msg = getMessage(key);
        return MessageFormat.format(msg, args);
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(getPrefix() + getMessage(key));
    }

    public void send(CommandSender sender, String key, Object... args) {
        sender.sendMessage(getPrefix() + getMessage(key, args));
    }

    public void send(Player player, String key) {
        player.sendMessage(getPrefix() + getMessage(key));
    }

    public void send(Player player, String key, Object... args) {
        player.sendMessage(getPrefix() + getMessage(key, args));
    }

    public void sendRaw(CommandSender sender, String key) {
        sender.sendMessage(getMessage(key));
    }

    public void sendRaw(CommandSender sender, String key, Object... args) {
        sender.sendMessage(getMessage(key, args));
    }
}