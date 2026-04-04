package com.inf2z.reimagined_world_selection.mixin;

import com.inf2z.reimagined_world_selection.screen.ReimaginedSelectWorldScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListEntryMixin {
    @Unique
    private long reimagined$lastClickTime;

    @SuppressWarnings("all")
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        ci.cancel();

        WorldListEntryAccessor acc = (WorldListEntryAccessor) this;
        LevelSummary s = acc.getSummary();
        WorldSelectionList parentList = acc.getList();
        Font f = Minecraft.getInstance().font;
        String name = s.getLevelName().isEmpty() ? s.getLevelId() : s.getLevelName();
        boolean isSelected = parentList.getSelected() == (Object) this;
        int effectiveWidth = width - 4;

        if (isSelected) {
            graphics.renderOutline(left - 1, top - 1, effectiveWidth + 2, height + 2, 0xFFFFFFFF);
        }
        if (hovered) {
            graphics.fill(left, top, left + effectiveWidth, top + height, 0x30FFFFFF);
        }

        if (Minecraft.getInstance().screen instanceof ReimaginedSelectWorldScreen screen) {
            graphics.blit(screen.loadIcon(s), left + 4, top, 0, 0, 32, 32, 32, 32);
        }

        int maxW = effectiveWidth - 50;
        int tw = f.width(name);
        float scale = tw > maxW ? (float) maxW / tw : 1.0f;
        graphics.pose().pushPose();
        float textY = top + (height - (f.lineHeight * scale)) / 2f;
        graphics.pose().translate(left + 44, textY, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(f, name, 0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        WorldSelectionList.WorldListEntry entry = (WorldSelectionList.WorldListEntry)(Object)this;
        WorldListEntryAccessor acc = (WorldListEntryAccessor) this;

        long currentTime = Util.getMillis();
        if (currentTime - this.reimagined$lastClickTime < 250L) {
            entry.joinWorld();
        } else {
            acc.getList().setSelected(entry);
        }

        this.reimagined$lastClickTime = currentTime;
        cir.setReturnValue(true);
    }
}