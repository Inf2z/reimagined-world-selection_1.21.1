package com.inf2z.reimagined_world_selection.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static void saveCurrent(@Nullable String var1, @Nullable String var2, @Nullable String var3) {
        saveCurrentTo("world", var1, var2, var3, null, null, null);
    }

    public static void saveCurrentExtended(@Nullable String var1, @Nullable String var2, @Nullable String var3, @Nullable String var4, @Nullable String var5, @Nullable String var6) {
        saveCurrentTo("world", var1, var2, var3, var4, var5, var6);
    }

    public static void saveCurrentTo(String storageType, @Nullable String var1, @Nullable String var2, @Nullable String var3, @Nullable String var4, @Nullable String var5, @Nullable String var6) {
        StorageContext context = getWritableContext(storageType);
        if (context == null) return;

        Map<String, String> vars = getVariablesInternal(context.worldId(), context.storageKey());

        applyVarIfNotFrozen(vars, 1, var1);
        applyVarIfNotFrozen(vars, 2, var2);
        applyVarIfNotFrozen(vars, 3, var3);
        applyVarIfNotFrozen(vars, 4, var4);
        applyVarIfNotFrozen(vars, 5, var5);
        applyVarIfNotFrozen(vars, 6, var6);

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

    public static Map<String, String> getWorldVariables(String worldId) {
        return getVariablesInternal(worldId, "save_folder");
    }

    public static Map<String, String> getPlayerVariables(String worldId) {
        String playerKey = getCurrentPlayerStorageKey();
        if (playerKey == null) return createDefaultVars();
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

    private static Map<String, String> getVariablesInternal(String worldId, String storageKey) {
        String cacheKey = worldId + "|" + storageKey;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        Map<String, String> vars = createDefaultVars();
        Path path = resolveStoragePath(worldId, storageKey);

        try {
            if (Files.exists(path)) {
                readVarsFromPath(vars, path);
            } else if ("save_folder".equals(storageKey)) {
                Path legacyPath = Minecraft.getInstance().gameDirectory.toPath()
                        .resolve("saves").resolve(worldId).resolve("reimagined_vars.json");
                if (Files.exists(legacyPath)) {
                    readVarsFromPath(vars, legacyPath);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to read vars for world {} and storage {}", worldId, storageKey, e);
        }

        cache.put(cacheKey, vars);
        return vars;
    }

    private static void readVarsFromPath(Map<String, String> vars, Path path) throws Exception {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null) {
                for (int i = 1; i <= 6; i++) {
                    if (json.has("var" + i)) vars.put("var" + i, json.get("var" + i).getAsString());
                    if (json.has("frozen" + i)) vars.put("frozen" + i, json.get("frozen" + i).getAsString());
                }
            }
        }
    }

    private static void saveToDisk(String worldId, String storageKey, Map<String, String> vars) {
        try {
            Path path = resolveStoragePath(worldId, storageKey);
            Files.createDirectories(path.getParent());

            JsonObject json = new JsonObject();
            for (int i = 1; i <= 6; i++) {
                json.addProperty("var" + i, vars.getOrDefault("var" + i, ""));
                json.addProperty("frozen" + i, vars.getOrDefault("frozen" + i, "false"));
            }

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save vars for world {} and storage {}", worldId, storageKey, e);
        }
    }

    private static Map<String, String> createDefaultVars() {
        Map<String, String> vars = new HashMap<>();
        for (int i = 1; i <= 6; i++) {
            vars.put("var" + i, "");
            vars.put("frozen" + i, "false");
        }
        return vars;
    }

    private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static double parseNumber(@Nullable String value) {
        try {
            if (value != null && !value.isEmpty()) {
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException ignored) {
        }
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
        return Minecraft.getInstance().getUser().getProfileId().toString();
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
        if ("world".equalsIgnoreCase(storageType)) {
            return "save_folder";
        }
        if ("player".equalsIgnoreCase(storageType)) {
            return getCurrentPlayerStorageKey();
        }
        return null;
    }

    private static Path resolveStoragePath(String worldId, String storageKey) {
        Path worldDir = Minecraft.getInstance().gameDirectory.toPath().resolve("saves").resolve(worldId);

        if ("save_folder".equals(storageKey)) {
            return worldDir.resolve("reimagined_vars.json");
        }

        return worldDir.resolve("reimagined_player_vars").resolve(storageKey + ".json");
    }

    private record StorageContext(String worldId, String storageKey) {}
}