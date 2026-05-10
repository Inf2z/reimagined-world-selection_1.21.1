package com.inf2z.reimagined_world_selection;

import com.mojang.blaze3d.platform.NativeImage;
import com.inf2z.reimagined_world_selection.util.ScreenshotCompat;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class WorldScreenshotHandler {
    public static void takeScreenshot(String worldId) {
        Minecraft mc = Minecraft.getInstance();

        try {
            Path worldPath = mc.gameDirectory.toPath()
                    .resolve("saves")
                    .resolve(worldId);

            if (!Files.exists(worldPath)) {
                return;
            }

            Path screenshotPath = worldPath.resolve("large_icon.png");

            boolean success = ScreenshotCompat.takeScreenshot(mc, image -> saveScreenshotAsync(image, screenshotPath));

            if (!success) {
            }
        } catch (Exception e) {
        }
    }

    private static void saveScreenshotAsync(NativeImage image, Path path) {
        Thread saveThread = new Thread(() -> {
            try {
                image.writeToFile(path);
            } catch (Exception e) {
            } finally {
                image.close();
            }
        });
        saveThread.setName("World Screenshot Saver");
        saveThread.start();
    }
}