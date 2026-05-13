package com.inf2z.reimagined_world_select.api;

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
public class RWSCustomInfoAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(RWSCustomInfoAPI.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Map<String, String>> cache = new HashMap<>();

    public static void varSetValue(int varNumber, String storageType, @Nullable String value) {
        StorageContext context = getWritableContext(storageType);
        if (context == null) return;

        Map<String, String> vars = getVariablesInternal(context.worldId(), context.storageType());
        if (isFrozen(vars, varNumber)) {
            LOGGER.debug("Variable {} is frozen, skipping set", varNumber);
            return;
        }

        vars.put("var" + varNumber, safe(value));
        saveToDisk(context.worldId(), context.storageType(), vars);
    }

    public static void varAddValue(int varNumber, String storageType, @Nullable String value) {
        StorageContext context = getWritableContext(storageType);
        if (context == null) return;

        Map<String, String> vars = getVariablesInternal(context.worldId(), context.storageType());
        if (isFrozen(vars, varNumber)) {
            LOGGER.debug("Variable {} is frozen, skipping add", varNumber);
            return;
        }

        double addAmount;
        try {
            addAmount = Double.parseDouble(safe(value));
        } catch (NumberFormatException e) {
            LOGGER.warn("varAddValue: invalid numeric value '{}' for variable {}, operation skipped", value, varNumber);
            return;
        }

        String currentRaw = vars.getOrDefault("var" + varNumber, "0");
        if (currentRaw.isEmpty()) currentRaw = "0";

        double current;
        try {
            current = Double.parseDouble(currentRaw);
        } catch (NumberFormatException e) {
            LOGGER.warn("varAddValue: variable {} contains non-numeric value '{}', operation skipped", varNumber, currentRaw);
            return;
        }

        double result = current + addAmount;
        String formatted = (result == (long) result) ? String.valueOf((long) result) : String.valueOf(result);

        vars.put("var" + varNumber, formatted);
        saveToDisk(context.worldId(), context.storageType(), vars);
    }

    public static void varFreeze(int varNumber, boolean frozen) {
        for (String type : new String[]{"world", "player"}) {
            StorageContext context = getWritableContext(type);
            if (context == null) continue;

            Map<String, String> vars = getVariablesInternal(context.worldId(), context.storageType());
            if (frozen) {
                vars.put("frozen" + varNumber, "true");
            } else {
                vars.remove("frozen" + varNumber);
            }
            saveToDisk(context.worldId(), context.storageType(), vars);
        }
    }

    @Nonnull
    public static String varGetValue(int varNumber, String storageType) {
        StorageContext context = getReadableContext(storageType);
        if (context == null) return "";

        return getVariablesInternal(context.worldId(), context.storageType()).getOrDefault("var" + varNumber, "");
    }

    @Nonnull
    public static Map<String, String> getWorldVariables(@Nullable String worldId) {
        if (worldId == null || worldId.isEmpty()) return new HashMap<>();
        return new HashMap<>(getVariablesInternal(worldId, "world"));
    }

    @Nonnull
    public static Map<String, String> getPlayerVariables(@Nullable String worldId) {
        if (worldId == null || worldId.isEmpty()) return new HashMap<>();
        String playerKey = getCurrentPlayerStorageKey();
        if (playerKey == null) return new HashMap<>();
        return new HashMap<>(getVariablesInternal(worldId, "player"));
    }

    public static void clearCache() {
        cache.clear();
    }

    private static boolean isFrozen(Map<String, String> vars, int varNumber) {
        return "true".equals(vars.get("frozen" + varNumber));
    }

    @Nonnull
    private static Map<String, String> getVariablesInternal(String worldId, String storageType) {
        String resolvedKey = resolveFileKey(storageType);
        if (resolvedKey == null) return new HashMap<>();

        String cacheKey = worldId + "|" + storageType + "|" + resolvedKey;
        if (cache.containsKey(cacheKey)) {
            Map<String, String> cached = cache.get(cacheKey);
            return cached != null ? cached : new HashMap<>();
        }

        Map<String, String> vars = new HashMap<>();
        try {
            Path path = resolveStoragePath(worldId, storageType, resolvedKey);
            if (path != null && Files.exists(path)) {
                readVarsFromPath(vars, path);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to read vars for world '{}', type '{}', key '{}'", worldId, storageType, resolvedKey);
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

    private static void saveToDisk(String worldId, String storageType, Map<String, String> vars) {
        String resolvedKey = resolveFileKey(storageType);
        if (resolvedKey == null) return;

        try {
            Path path = resolveStoragePath(worldId, storageType, resolvedKey);
            if (path == null) return;

            Files.createDirectories(path.getParent());

            JsonObject json = new JsonObject();
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                json.addProperty(entry.getKey(), entry.getValue());
            }

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(json, writer);
            }

            String cacheKey = worldId + "|" + storageType + "|" + resolvedKey;
            cache.put(cacheKey, vars);
        } catch (Exception e) {
            LOGGER.error("Failed to save vars for world '{}', type '{}'", worldId, storageType);
        }
    }

    @Nullable
    private static Path resolveStoragePath(String worldId, String storageType, String resolvedKey) {
        try {
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            Path worldDir = gameDir.resolve("saves").resolve(worldId);
            Path rwsVarsDir = worldDir.resolve("rws_vars");

            if ("world".equalsIgnoreCase(storageType)) {
                return rwsVarsDir.resolve("world").resolve("vars.json");
            }
            if ("player".equalsIgnoreCase(storageType)) {
                return rwsVarsDir.resolve("player").resolve(resolvedKey + ".json");
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to resolve storage path");
        }
        return null;
    }

    @Nullable
    private static String resolveFileKey(String storageType) {
        if ("world".equalsIgnoreCase(storageType)) return "world";
        if ("player".equalsIgnoreCase(storageType)) return getCurrentPlayerStorageKey();
        LOGGER.warn("Unknown storage type: '{}', expected 'world' or 'player'", storageType);
        return null;
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
        if (worldId == null) {
            LOGGER.debug("Cannot access vars: no singleplayer world loaded");
            return null;
        }
        return new StorageContext(worldId, storageType);
    }

    @Nullable
    private static StorageContext getReadableContext(String storageType) {
        return getWritableContext(storageType);
    }

    private record StorageContext(String worldId, String storageType) {}
}