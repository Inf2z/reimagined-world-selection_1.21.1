package com.inf2z.reimagined_world_selection.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

public class WorldInfoHelper {
    public record DifficultyInfo(int difficultyId, boolean locked) {}

    @Nullable
    public static DifficultyInfo readDifficulty(Path worldDir) {
        Path levelDat = worldDir.resolve("level.dat");
        if (!Files.exists(levelDat)) return null;

        try (InputStream fileInput = Files.newInputStream(levelDat);
             GZIPInputStream gzipInput = new GZIPInputStream(fileInput);
             DataInputStream dataInput = new DataInputStream(gzipInput)) {

            CompoundTag root = readNbt(dataInput);
            if (root == null || !nbtContains(root, "Data", 10)) return null;

            CompoundTag data = root.getCompound("Data");
            int difficulty = nbtContains(data, "Difficulty", 99) ? data.getByte("Difficulty") : 2;
            boolean locked = nbtContains(data, "DifficultyLocked", 1) && data.getBoolean("DifficultyLocked");

            return new DifficultyInfo(difficulty, locked);
        } catch (Exception e) {
            return null;
        }
    }

    public static long readPlayerPlayTime(Path worldDir) {
        try {
            User user = Minecraft.getInstance().getUser();
            UUID uuid = user.getProfileId();
            if (uuid == null) return 0L;

            Path statsPath = worldDir.resolve("stats").resolve(uuid + ".json");
            if (!Files.exists(statsPath)) return 0L;

            try (BufferedReader reader = Files.newBufferedReader(statsPath)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) return 0L;

                JsonObject root = element.getAsJsonObject();
                if (!root.has("stats")) return 0L;

                JsonObject stats = root.getAsJsonObject("stats");
                if (!stats.has("minecraft:custom")) return 0L;

                JsonObject custom = stats.getAsJsonObject("minecraft:custom");

                if (custom.has("minecraft:play_time")) {
                    return custom.get("minecraft:play_time").getAsLong();
                }

                if (custom.has("minecraft:play_one_minute")) {
                    return custom.get("minecraft:play_one_minute").getAsLong();
                }

                return 0L;
            }
        } catch (Exception e) {
            return 0L;
        }
    }

    private static boolean nbtContains(CompoundTag tag, String key, int type) {
        try {
            Method m = CompoundTag.class.getMethod("contains", String.class, int.class);
            return (boolean) m.invoke(tag, key, type);
        } catch (Throwable ignored) {
        }

        try {
            Method m = CompoundTag.class.getMethod("contains", String.class);
            return (boolean) m.invoke(tag, key);
        } catch (Throwable ignored) {
        }

        return false;
    }

    @Nullable
    private static CompoundTag readNbt(DataInputStream input) {
        for (String methodName : new String[]{"read", "readCompressed"}) {
            try {
                Method m = NbtIo.class.getMethod(methodName, DataInputStream.class, findNbtAccounter());
                Object accounter = createUnlimitedAccounter();
                if (accounter != null) {
                    return (CompoundTag) m.invoke(null, input, accounter);
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            Method m = NbtIo.class.getMethod("read", DataInputStream.class);
            return (CompoundTag) m.invoke(null, input);
        } catch (Throwable ignored) {
        }

        for (Method m : NbtIo.class.getDeclaredMethods()) {
            Class<?>[] params = m.getParameterTypes();
            if (m.getReturnType() == CompoundTag.class && params.length >= 1) {
                if (params[0] == DataInputStream.class) {
                    try {
                        m.setAccessible(true);
                        if (params.length == 1) {
                            return (CompoundTag) m.invoke(null, input);
                        }
                        if (params.length == 2) {
                            Object accounter = createUnlimitedAccounter();
                            if (accounter != null) {
                                return (CompoundTag) m.invoke(null, input, accounter);
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        return null;
    }

    private static Class<?> findNbtAccounter() {
        String[] candidates = {
                "net.minecraft.nbt.NbtAccounter",
                "net.minecraft.nbt.NbtTagSizeTracker"
        };
        for (String name : candidates) {
            try {
                return Class.forName(name);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Nullable
    private static Object createUnlimitedAccounter() {
        Class<?> cls = findNbtAccounter();
        if (cls == null) return null;

        String[] methodNames = {"unlimitedHeap", "unlimited", "create"};
        for (String name : methodNames) {
            try {
                Method m = cls.getMethod(name);
                return m.invoke(null);
            } catch (Throwable ignored) {
            }
        }

        try {
            return cls.getConstructor(long.class).newInstance(Long.MAX_VALUE);
        } catch (Throwable ignored) {
        }

        return null;
    }
}