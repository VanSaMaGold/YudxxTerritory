package yudxx.minecraft.spigot.yudxxTerritory.manager;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    private final JavaPlugin plugin;
    private final Map<String, Sound> soundMap;
    private boolean enabled;

    public SoundManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.soundMap = new HashMap<>();
        this.enabled = true;
    }

    public void loadSounds() {
        soundMap.clear();
        File soundsFile = new File(plugin.getDataFolder(), "sounds.yml");
        if (!soundsFile.exists()) {
            plugin.saveResource("sounds.yml", false);
        }
        FileConfiguration soundsConfig = YamlConfiguration.loadConfiguration(soundsFile);

        ConfigurationSection soundsSection = soundsConfig.getConfigurationSection("sounds");
        if (soundsSection == null) {
            enabled = false;
            return;
        }

        for (String key : soundsSection.getKeys(false)) {
            String soundName = soundsSection.getString(key);
            if (soundName == null || soundName.equalsIgnoreCase("none")) continue;
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                soundMap.put(key, sound);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void playSound(Player player, String key) {
        if (!enabled) return;
        Sound sound = soundMap.get(key);
        if (sound == null) return;
        try {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
    }

    public void playSound(Player player, String key, float volume, float pitch) {
        if (!enabled) return;
        Sound sound = soundMap.get(key);
        if (sound == null) return;
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception ignored) {
        }
    }

    public void reloadSounds() {
        loadSounds();
    }
}