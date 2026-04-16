package com.inf2z.reimagined_world_selection.mixin;

import com.inf2z.reimagined_world_selection.WorldScreenshotHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.storage.LevelResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(Minecraft.class)
public class WorldExitMixin {

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void reimagined$beforeSetScreen(Screen screen, CallbackInfo ci) {
        Minecraft mc = (Minecraft)(Object)this;

        if (screen instanceof PauseScreen && mc.level != null && mc.getSingleplayerServer() != null) {
            try {
                MinecraftServerAccessor serverAccessor = (MinecraftServerAccessor) mc.getSingleplayerServer();
                Path levelPath = serverAccessor.getStorageSource()
                        .getLevelPath(LevelResource.ROOT)
                        .normalize();
                String worldId = levelPath.getFileName().toString();
                WorldScreenshotHandler.takeScreenshot(worldId);
            } catch (Exception e) {
                String worldId = mc.getSingleplayerServer().getWorldData().getLevelName();
                WorldScreenshotHandler.takeScreenshot(worldId);
            }
        }
    }
}