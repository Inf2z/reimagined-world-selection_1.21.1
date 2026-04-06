package com.inf2z.reimagined_world_selection.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
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

            CompoundTag root = NbtIo.read(dataInput, NbtAccounter.unlimitedHeap());
            if (root == null || !root.contains("Data", 10)) return null;

            CompoundTag data = root.getCompound("Data");
            int difficulty = data.contains("Difficulty", 99) ? data.getByte("Difficulty") : 2;
            boolean locked = data.contains("DifficultyLocked", 1) && data.getBoolean("DifficultyLocked");

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

            Path statsPath = worldDir.resolve("stats").resolve(uuid.toString() + ".json");
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
}