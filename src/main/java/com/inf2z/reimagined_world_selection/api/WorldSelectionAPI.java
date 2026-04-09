package com.inf2z.reimagined_world_selection.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class WorldSelectionAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldSelectionAPI.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Map<String, String>> cache = new HashMap<>();

    @Deprecated
    public static void saveCurrent(@Nullable String var1, @Nullable String var2, @Nullable String var3) {
        saveCurrentExtended(var1, var2, var3, null, null, null);
    }

    @Deprecated
    public static void saveCurrentExtended(@Nullable String var1, @Nullable String var2, @Nullable String var3,
                                           @Nullable String var4, @Nullable String var5, @Nullable String var6) {
        Map<String, String> vars = new HashMap<>();
        vars.put("1", safe(var1));
        vars.put("2", safe(var2));
        vars.put("3", safe(var3));
        vars.put("4", safe(var4));
        vars.put("5", safe(var5));
        vars.put("6", safe(var6));

        saveCurrentTo("world", vars);
    }

    public static void saveCurrentTo(String storageType, Map<String, String> variables) {
        StorageContext context = getWritableContext(storageType);
        if (context == null) return;

        Map<String, String> vars = getVariablesInternal(context.worldId(), context.storageKey());

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            try {
                int lineNumber = Integer.parseInt(entry.getKey());
                applyVarIfNotFrozen(vars, lineNumber, entry.getValue());
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid line number: {}", entry.getKey());
            }
        }

        saveToDisk(context.worldId(), context.storageKey(), vars);
    }

    public static void updateVar(int lineNumber, String storageType, @Nullable String value) {
        StorageContext context = getWritableContext(storageType);
        if (context == null) return;

        Map<String, String> vars = getVariablesInternal(context.worldId(), context.storageKey());
        if (isFrozen(vars, lineNumber)) return;

        vars.put("var" + lineNumber, safe(value));
        saveToDisk(context.worldId(), context.storageKey(), vars);
    }

    public static void addValue(int lineNumber, String storageType, double amount) {
        StorageContext context = getWritableContext(storageType);
        if (context == null) return;

        Map<String, String> vars = getVariablesInternal(context.worldId(), context.storageKey());
        if (isFrozen(vars, lineNumber)) return;

        double currentNum = parseNumber(vars.get("var" + lineNumber));
        double newVal = currentNum + amount;
        String formattedVal = (newVal == (long) newVal) ? String.valueOf((long) newVal) : String.valueOf(newVal);

        vars.put("var" + lineNumber, formattedVal);
        saveToDisk(context.worldId(), context.storageKey(), vars);
    }

    public static void resetVar(int lineNumber, String storageType) {
        updateVar(lineNumber, storageType, "0");
    }

    @Nonnull
    public static String getString(int lineNumber, String storageType) {
        StorageContext context = getReadableContext(storageType);
        if (context == null) return "";

        return getVariablesInternal(context.worldId(), context.storageKey()).getOrDefault("var" + lineNumber, "");
    }

    public static double getNumber(int lineNumber, String storageType) {
        return parseNumber(getString(lineNumber, storageType));
    }

    public static void freeze(int lineNumber, String storageType) {
        setFrozenState(lineNumber, storageType, true);
    }

    public static void unfreeze(int lineNumber, String storageType) {
        setFrozenState(lineNumber, storageType, false);
    }

    @Nonnull
    public static Map<String, String> getWorldVariables(@Nullable String worldId) {
        if (worldId == null || worldId.isEmpty()) return new HashMap<>();
        return getVariablesInternal(worldId, "save_folder");
    }

    @Nonnull
    public static Map<String, String> getPlayerVariables(@Nullable String worldId) {
        if (worldId == null || worldId.isEmpty()) return new HashMap<>();
        String playerKey = getCurrentPlayerStorageKey();
        if (playerKey == null) return new HashMap<>();
        return getVariablesInternal(worldId, playerKey);
    }

    public static void clearCache() {
        cache.clear();
    }

    private static void setFrozenState(int lineNumber, String storageType, boolean state) {
        StorageContext context = getWritableContext(storageType);
        if (context == null) return;

        Map<String, String> vars = getVariablesInternal(context.worldId(), context.storageKey());
        vars.put("frozen" + lineNumber, String.valueOf(state));
        saveToDisk(context.worldId(), context.storageKey(), vars);
    }

    private static boolean isFrozen(Map<String, String> vars, int lineNumber) {
        return Boolean.parseBoolean(vars.getOrDefault("frozen" + lineNumber, "false"));
    }

    private static void applyVarIfNotFrozen(Map<String, String> vars, int lineNumber, @Nullable String value) {
        if (!isFrozen(vars, lineNumber)) {
            vars.put("var" + lineNumber, safe(value));
        }
    }

    @Nonnull
    private static Map<String, String> getVariablesInternal(String worldId, String storageKey) {
        String cacheKey = worldId + "|" + storageKey;
        if (cache.containsKey(cacheKey)) {
            Map<String, String> cached = cache.get(cacheKey);
            return cached != null ? cached : new HashMap<>();
        }

        Map<String, String> vars = new HashMap<>();
        try {
            Path path = resolveStoragePath(worldId, storageKey);
            if (path != null && Files.exists(path)) {
                readVarsFromPath(vars, path);
            } else if ("save_folder".equals(storageKey)) {
                Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
                Path legacyPath = gameDir.resolve("saves").resolve(worldId).resolve("reimagined_vars.json");
                if (Files.exists(legacyPath)) {
                    readVarsFromPath(vars, legacyPath);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to read vars for world {} and storage {}", worldId, storageKey);
        }

        cache.put(cacheKey, vars);
        return vars;
    }

    private static void readVarsFromPath(Map<String, String> vars, Path path) throws Exception {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null) {
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    vars.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
    }

    private static void saveToDisk(String worldId, String storageKey, Map<String, String> vars) {
        try {
            Path path = resolveStoragePath(worldId, storageKey);
            if (path == null) return;

            Files.createDirectories(path.getParent());

            JsonObject json = new JsonObject();
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                json.addProperty(entry.getKey(), entry.getValue());
            }

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save vars for world {}", worldId);
        }
    }

    private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static double parseNumber(@Nullable String value) {
        try {
            if (value != null && !value.isEmpty()) {
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException ignored) {}
        return 0.0;
    }

    @Nullable
    private static String getCurrentWorldId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            return mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        return null;
    }

    @Nullable
    private static String getCurrentPlayerStorageKey() {
        try {
            return Minecraft.getInstance().getUser().getProfileId().toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static StorageContext getWritableContext(String storageType) {
        String worldId = getCurrentWorldId();
        if (worldId == null) return null;

        String storageKey = resolveStorageKey(storageType);
        if (storageKey == null) return null;

        return new StorageContext(worldId, storageKey);
    }

    @Nullable
    private static StorageContext getReadableContext(String storageType) {
        return getWritableContext(storageType);
    }

    @Nullable
    private static String resolveStorageKey(String storageType) {
        if ("world".equalsIgnoreCase(storageType)) return "save_folder";
        if ("player".equalsIgnoreCase(storageType)) return getCurrentPlayerStorageKey();
        return null;
    }

    @Nullable
    private static Path resolveStoragePath(String worldId, String storageKey) {
        try {
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            Path worldDir = gameDir.resolve("saves").resolve(worldId);

            if ("save_folder".equals(storageKey)) {
                return worldDir.resolve("reimagined_vars.json");
            }

            return worldDir.resolve("reimagined_player_vars").resolve(storageKey + ".json");
        } catch (Exception e) {
            return null;
        }
    }

    private record StorageContext(String worldId, String storageKey) {}
}