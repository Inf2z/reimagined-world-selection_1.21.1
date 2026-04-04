package com.inf2z.reimagined_world_selection;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class WorldScreenshotHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldScreenshotHandler.class);

    public static void takeScreenshot(String worldId) {
        Minecraft mc = Minecraft.getInstance();

        System.out.println("Taking screenshot for world: " + worldId);

        try {
            Path worldPath = mc.gameDirectory.toPath()
                    .resolve("saves")
                    .resolve(worldId);

            if (!Files.exists(worldPath)) {
                System.out.println("World path does not exist!");
                return;
            }

            Path screenshotPath = worldPath.resolve("large_icon.png");
            System.out.println("Screenshot will be saved to: " + screenshotPath);

            NativeImage screenshot = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            saveScreenshotAsync(screenshot, screenshotPath);

            LOGGER.info("Taking screenshot for world: {}", worldId);

        } catch (Exception e) {
            LOGGER.error("Failed to take world screenshot", e);
            e.printStackTrace();
        }
    }

    private static void saveScreenshotAsync(NativeImage image, Path path) {
        Consumer<NativeImage> saver = (img) -> {
            try {
                img.writeToFile(path);
                System.out.println("Successfully saved screenshot to: " + path);
                LOGGER.info("Saved world screenshot to: {}", path);
            } catch (Exception e) {
                System.out.println("Failed to save screenshot: " + e.getMessage());
                LOGGER.error("Failed to save world screenshot", e);
                e.printStackTrace();
            } finally {
                img.close();
            }
        };

        Thread saveThread = new Thread(() -> saver.accept(image));
        saveThread.setName("World Screenshot Saver");
        saveThread.start();
    }
}