package com.inf2z.reimagined_world_selection.mixin;

import com.inf2z.reimagined_world_selection.screen.ReimaginedSelectWorldScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SelectWorldScreen.class)
public class SelectWorldMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructed(CallbackInfo ci) {
        Object current = this;

        if (current.getClass().getName().equals("net.minecraft.client.gui.screens.worldselection.SelectWorldScreen")) {
            Minecraft mc = Minecraft.getInstance();

            mc.tell(() -> {
                if (!(mc.screen instanceof ReimaginedSelectWorldScreen)) {
                    mc.setScreen(new ReimaginedSelectWorldScreen(null));
                }
            });
        }
    }
}