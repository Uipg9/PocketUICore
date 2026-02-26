package com.pocketuicore.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client-side key–value persistence backed by a JSON file in
 * {@code .minecraft/config/<namespace>.json}.
 * <p>
 * Each mod that needs client settings creates its own instance with a
 * unique namespace. Values are written to disk automatically on every
 * {@link #set} call (auto-save). Versioned data migration is supported.
 * <p>
 * <b>This is distinct from the server-side PocketDataStore</b> — it
 * stores UI preferences, last-opened-tab indices, toggle states, and
 * other lightweight client data.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     UIDataStore store = new UIDataStore("mymod_ui");
 *     store.set("lastTab", 2);
 *     int tab = store.getInt("lastTab", 0);
 * }</pre>
 */
public final class UIDataStore {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final String VERSION_KEY = "__version";

    private final String namespace;
    private final Path filePath;
    private final Map<String, Object> data = new HashMap<>();
    private int dataVersion = 1;
    private Consumer<UIDataStore> migrationHandler;

    // =====================================================================
    //  Construction & loading
    // =====================================================================

    /**
     * Create or load a data store.
     *
     * @param namespace unique identifier — becomes the JSON filename
     */
    public UIDataStore(String namespace) {
        this.namespace = namespace;
        this.filePath  = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(namespace + ".json");
        load();
    }

    /**
     * Register a migration handler that is called when the on-disk data
     * version is lower than the current {@link #setDataVersion version}.
     *
     * @param handler receives this store so it can read/remove/write keys
     */
    public UIDataStore onMigrate(Consumer<UIDataStore> handler) {
        this.migrationHandler = handler;
        return this;
    }

    /**
     * Set the expected data version. If the on-disk version is lower, the
     * {@link #onMigrate migration handler} (if any) will be called.
     *
     * @param version integer version number (default 1)
     */
    public UIDataStore setDataVersion(int version) {
        int oldVersion = this.dataVersion;
        this.dataVersion = version;
        // Trigger migration if on-disk version is older
        int diskVersion = getInt(VERSION_KEY, 1);
        if (diskVersion < version && migrationHandler != null) {
            migrationHandler.accept(this);
            set(VERSION_KEY, version);
        }
        return this;
    }

    // =====================================================================
    //  Getters — type-safe with defaults
    // =====================================================================

    public String getString(String key, String defaultValue) {
        Object o = data.get(key);
        return o != null ? o.toString() : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object o = data.get(key);
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public long getLong(String key, long defaultValue) {
        Object o = data.get(key);
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public float getFloat(String key, float defaultValue) {
        Object o = data.get(key);
        if (o instanceof Number n) return n.floatValue();
        if (o instanceof String s) {
            try { return Float.parseFloat(s); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public double getDouble(String key, double defaultValue) {
        Object o = data.get(key);
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object o = data.get(key);
        if (o instanceof Boolean b) return b;
        if (o instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }

    /** @return {@code true} if the key exists in the store. */
    public boolean has(String key) {
        return data.containsKey(key);
    }

    // =====================================================================
    //  Setters — auto-save
    // =====================================================================

    /**
     * Set a value and persist to disk immediately.
     *
     * @param key   the key
     * @param value any JSON-serialisable value (String, Number, Boolean)
     */
    public void set(String key, Object value) {
        data.put(key, value);
        save();
    }

    /** Remove a key and persist. */
    public void remove(String key) {
        data.remove(key);
        save();
    }

    /** Clear all data and persist. */
    public void clear() {
        data.clear();
        data.put(VERSION_KEY, dataVersion);
        save();
    }

    // =====================================================================
    //  Persistence (JSON file)
    // =====================================================================

    private void load() {
        if (!Files.exists(filePath)) return;
        try {
            String json = Files.readString(filePath);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                JsonElement el = entry.getValue();
                if (el.isJsonPrimitive()) {
                    var prim = el.getAsJsonPrimitive();
                    if (prim.isBoolean()) {
                        data.put(entry.getKey(), prim.getAsBoolean());
                    } else if (prim.isNumber()) {
                        data.put(entry.getKey(), prim.getAsNumber());
                    } else {
                        data.put(entry.getKey(), prim.getAsString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[PocketUICore] Failed to load UIDataStore '"
                    + namespace + "': " + e.getMessage());
        }
    }

    private void save() {
        try {
            // Ensure parent dirs exist
            Files.createDirectories(filePath.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty(VERSION_KEY, dataVersion);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                if (val instanceof Boolean b) obj.addProperty(key, b);
                else if (val instanceof Number n) obj.addProperty(key, n);
                else obj.addProperty(key, val.toString());
            }
            Files.writeString(filePath, GSON.toJson(obj));
        } catch (IOException e) {
            System.err.println("[PocketUICore] Failed to save UIDataStore '"
                    + namespace + "': " + e.getMessage());
        }
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    /** @return the namespace for this store. */
    public String getNamespace() { return namespace; }

    /** @return absolute path to the backing JSON file. */
    public Path getFilePath() { return filePath; }

    /** @return the number of stored keys (excluding internal keys). */
    public int size() {
        return (int) data.keySet().stream()
                .filter(k -> !k.startsWith("__"))
                .count();
    }
}
