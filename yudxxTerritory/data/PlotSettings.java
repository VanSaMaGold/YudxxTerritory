package yudxx.minecraft.spigot.yudxxTerritory.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class PlotSettings {

    private static final Gson GSON = new Gson();

    private final Map<String, Object> settings;

    public PlotSettings() {
        this.settings = new HashMap<>();
        setDefaults();
    }

    private void setDefaults() {
        settings.put("time", "default");
        settings.put("weather", "default");
        settings.put("pvp", true);
        settings.put("mob-spawning", true);
        settings.put("animal-spawning", true);
        settings.put("fire-spread", false);
        settings.put("explosions", false);
        settings.put("enter-message", true);
        settings.put("leave-message", true);
        settings.put("gamemode", "default");
        settings.put("monster-spawning", true);
        settings.put("item-drops", true);
        settings.put("item-discard", true);
    }

    public void set(String key, Object value) {
        settings.put(key, value);
    }

    public Object get(String key) {
        return settings.get(key);
    }

    public String getString(String key) {
        Object val = settings.get(key);
        return val != null ? val.toString() : "default";
    }

    public boolean getBoolean(String key) {
        Object val = settings.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        return true;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public static String toJson(PlotSettings settings) {
        return GSON.toJson(settings.getSettings());
    }

    public static PlotSettings fromJson(String json) {
        PlotSettings settings = new PlotSettings();
        if (json != null && !json.isEmpty()) {
            try {
                Type type = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> loaded = GSON.fromJson(json, type);
                if (loaded != null) {
                    settings.getSettings().putAll(loaded);
                }
            } catch (Exception ignored) {
            }
        }
        return settings;
    }
}