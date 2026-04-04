package com.inf2z.reimagined_world_selection.mixin;

import com.inf2z.reimagined_world_selection.screen.ReimaginedSelectWorldScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldSelectionList.WorldListEntry.class)
public class WorldListEntryMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float tick, CallbackInfo ci) {
        ci.cancel();
        LevelSummary s = ((EntryAccessor) this).getSummary();
        Font f = Minecraft.getInstance().font;
        String name = s.getLevelName().isEmpty() ? s.getLevelId() : s.getLevelName();

        if (hovered) graphics.fill(left, top, left + width, top + height, 0x30FFFFFF);

        if (Minecraft.getInstance().screen instanceof ReimaginedSelectWorldScreen screen) {
            graphics.blit(screen.loadIcon(s), left + 4, top, 0, 0, 32, 32, 32, 32);
        }

        int maxW = width - 50;
        int tw = f.width(name);
        float scale = tw > maxW ? (float) maxW / tw : 1.0f;

        graphics.pose().pushPose();
        graphics.pose().translate(left + 44, top + (height - f.lineHeight * scale) / 2f, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(f, name, 0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
    }
}