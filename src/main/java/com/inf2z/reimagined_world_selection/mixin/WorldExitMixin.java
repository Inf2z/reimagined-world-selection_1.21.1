package com.inf2z.reimagined_world_selection.mixin;

import com.inf2z.reimagined_world_selection.WorldScreenshotHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class WorldExitMixin {

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void reimagined$beforeSetScreen(Screen screen, CallbackInfo ci) {
        Minecraft mc = (Minecraft)(Object)this;

        if (screen instanceof PauseScreen && mc.level != null && mc.getSingleplayerServer() != null) {
            String worldId = mc.getSingleplayerServer().getWorldData().getLevelName();
            System.out.println("Pause screen opening, taking screenshot before menu renders...");
            WorldScreenshotHandler.takeScreenshot(worldId);
        }
    }
}