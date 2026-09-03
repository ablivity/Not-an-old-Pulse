package ablivity.dev.not_an_old_pulse.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE;
    private final File configFile;
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Consumer<Object>> listeners = new HashMap<>();

    private ModConfig() {
        this.configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "not_an_old_pulse.json");
        loadDefaults();
        load();
    }

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
        }
        return INSTANCE;
    }

    private void loadDefaults() {
        values.put("fullBright", true);
        values.put("chinaHat", true);
        values.put("noHurtCam", true);
        values.put("hitboxCustomizer", true);
        values.put("particles", true);
        values.put("customHand", false);
        values.put("hitColor", true);
        values.put("jumpCircles", true);
        values.put("renderTweaks", true);
        values.put("aspectRatio", false);
        values.put("crosshair", false);
        values.put("motionBlur", false);
        values.put("motionBlur_Strength", 0.5f);
        values.put("fpsDisplay", true);
        values.put("cpsDisplay", true);
        values.put("pingDisplay", true);
        values.put("coords", true);
        values.put("speedometer", false);
        values.put("armorStatus", true);
        values.put("keystrokes", true);
        values.put("autoSprint", true);
        values.put("timeChanger", false);
        values.put("timeValue", 6000L);
        values.put("dynamicIsland", true);
        values.put("targetHud", true);
        values.put("customTab", true);
        values.put("hitSounds", true);
        values.put("playerTrail", true);
        values.put("friendProtection", false);
        values.put("playerInfo", true);
        values.put("cooldowns", true);
        values.put("potions", true);
        values.put("customFov", false);
        values.put("fovValue", 90.0);
        values.put("guiScale", 100);
        values.put("hud_x", 8);
        values.put("hud_y", 8);
        values.put("keystrokes_x", -1);
        values.put("keystrokes_y", 30);
    }

    public boolean get(String key) {
        return get(key, false);
    }

    public boolean get(String key, boolean def) {
        Object val = values.get(key);
        if (val == null) return def;
        if (val instanceof Boolean b) return b;
        if (val instanceof Number n) return n.intValue() != 0;
        if (val instanceof String s) {
            if ("false".equalsIgnoreCase(s) || "0".equals(s)) return false;
            if ("true".equalsIgnoreCase(s) || "1".equals(s)) return true;
        }
        return def;
    }

    public int getInt(String key, int def) {
        Object val = values.get(key);
        if (val instanceof Number n) return n.intValue();
        return def;
    }

    public long getLong(String key, long def) {
        Object val = values.get(key);
        if (val instanceof Number n) return n.longValue();
        return def;
    }

    public float getFloat(String key, float def) {
        Object val = values.get(key);
        if (val instanceof Number n) return n.floatValue();
        return def;
    }

    public double getDouble(String key, double def) {
        Object val = values.get(key);
        if (val instanceof Number n) return n.doubleValue();
        return def;
    }

    public String getString(String key, String def) {
        Object val = values.get(key);
        if (val != null) return val.toString();
        return def;
    }

    public void set(String key, Object value) {
        values.put(key, value);
        save();
        Consumer<Object> listener = listeners.get(key);
        if (listener != null) {
            listener.accept(value);
        }
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public void addListener(String key, Consumer<Object> listener) {
        listeners.put(key, listener);
    }

    public void removeListener(String key) {
        listeners.remove(key);
    }

    private void load() {
        if (!configFile.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                values.putAll(loaded);
            }
        } catch (Exception e) {
            save();
        }
    }

    public void save() {
        try {
            if (configFile.getParentFile() != null && !configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(values, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
