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
        String worldId = getCurrentWorldId();
        if (worldId == null) return;
        Map<String, String> vars = getVariables(worldId);

        if (!isFrozen(vars, 1)) vars.put("var1", var1 == null ? "" : var1);
        if (!isFrozen(vars, 2)) vars.put("var2", var2 == null ? "" : var2);
        if (!isFrozen(vars, 3)) vars.put("var3", var3 == null ? "" : var3);

        saveToDisk(worldId, vars);
    }

    public static void updateVar(int lineNumber, @Nullable String value) {
        String worldId = getCurrentWorldId();
        if (worldId == null) return;
        Map<String, String> vars = getVariables(worldId);

        if (isFrozen(vars, lineNumber)) return;

        vars.put("var" + lineNumber, value == null ? "" : value);
        saveToDisk(worldId, vars);
    }

    public static void addValue(int lineNumber, double amount) {
        String worldId = getCurrentWorldId();
        if (worldId == null) return;
        Map<String, String> vars = getVariables(worldId);

        if (isFrozen(vars, lineNumber)) return;

        double currentNum = getNumber(lineNumber);
        double newVal = currentNum + amount;

        String formattedVal = (newVal == (long) newVal) ? String.valueOf((long) newVal) : String.valueOf(newVal);

        vars.put("var" + lineNumber, formattedVal);
        saveToDisk(worldId, vars);
    }

    public static void resetVar(int lineNumber) {
        updateVar(lineNumber, "0");
    }

    public static String getString(int lineNumber) {
        String worldId = getCurrentWorldId();
        if (worldId == null) return "";
        return getVariables(worldId).getOrDefault("var" + lineNumber, "");
    }

    public static double getNumber(int lineNumber) {
        String str = getString(lineNumber);
        try {
            if (str != null && !str.isEmpty()) return Double.parseDouble(str);
        } catch (NumberFormatException e) { }
        return 0.0;
    }

    public static void freeze(int lineNumber) {
        setFrozenState(lineNumber, true);
    }

    public static void unfreeze(int lineNumber) {
        setFrozenState(lineNumber, false);
    }

    private static void setFrozenState(int lineNumber, boolean state) {
        String worldId = getCurrentWorldId();
        if (worldId == null) return;
        Map<String, String> vars = getVariables(worldId);

        vars.put("frozen" + lineNumber, String.valueOf(state));
        saveToDisk(worldId, vars);
    }

    private static boolean isFrozen(Map<String, String> vars, int lineNumber) {
        return Boolean.parseBoolean(vars.getOrDefault("frozen" + lineNumber, "false"));
    }

    public static Map<String, String> getVariables(String worldId) {
        if (cache.containsKey(worldId)) {
            return cache.get(worldId);
        }

        Map<String, String> vars = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            vars.put("var" + i, "");
            vars.put("frozen" + i, "false");
        }

        try {
            Path path = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("saves").resolve(worldId).resolve("reimagined_vars.json");

            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json != null) {
                        for (int i = 1; i <= 3; i++) {
                            if (json.has("var" + i)) vars.put("var" + i, json.get("var" + i).getAsString());
                            if (json.has("frozen" + i)) vars.put("frozen" + i, json.get("frozen" + i).getAsString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to read reimagined_vars.json for world: {}", worldId, e);
        }

        cache.put(worldId, vars);
        return vars;
    }

    public static void clearCache() {
        cache.clear();
    }

    private static void saveToDisk(String worldId, Map<String, String> vars) {
        try {
            Path path = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("saves").resolve(worldId).resolve("reimagined_vars.json");

            JsonObject json = new JsonObject();
            for (int i = 1; i <= 3; i++) {
                json.addProperty("var" + i, vars.getOrDefault("var" + i, ""));
                json.addProperty("frozen" + i, vars.getOrDefault("frozen" + i, "false"));
            }

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save reimagined_vars.json for world: {}", worldId, e);
        }
    }

    @Nullable
    private static String getCurrentWorldId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            return mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        return null;
    }
}