package com.inf2z.reimagined_world_selection.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin {

    @Inject(
            method = "renderSelection(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void rws$disableVanillaSelection(GuiGraphics graphics, int index, int width, int height, int borderColor, int backgroundColor, CallbackInfo ci) {
        if ((Object) this instanceof WorldSelectionList) {
            ci.cancel();
        }
    }
}