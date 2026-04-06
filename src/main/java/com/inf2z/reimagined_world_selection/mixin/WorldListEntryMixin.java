package com.inf2z.reimagined_world_selection.mixin;

import com.inf2z.reimagined_world_selection.Config;
import com.inf2z.reimagined_world_selection.screen.ReimaginedSelectWorldScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListEntryMixin {
    @Unique
    private static final Map<WorldSelectionList.WorldListEntry, Float> reimagined$selectAnimation = new WeakHashMap<>();

    @Unique
    private long reimagined$lastClickTime;

    @SuppressWarnings("all")
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        ci.cancel();

        WorldSelectionList.WorldListEntry self = (WorldSelectionList.WorldListEntry) (Object) this;
        WorldListEntryAccessor acc = (WorldListEntryAccessor) this;
        LevelSummary s = acc.getSummary();
        WorldSelectionList parentList = acc.getList();
        Font f = Minecraft.getInstance().font;

        String name = s.getLevelName().isEmpty() ? s.getLevelId() : s.getLevelName();
        boolean isSelected = parentList.getSelected() == self;

        float anim = reimagined$selectAnimation.getOrDefault(self, 0.0f);
        float target = isSelected ? 1.0f : 0.0f;
        anim = Mth.lerp(0.12f, anim, target);
        if (Math.abs(anim - target) < 0.01f) anim = target;
        reimagined$selectAnimation.put(self, anim);

        int effectiveWidth = width - 4;

        float worldExpand = Config.ENABLE_WORLD_HOVER_ANIMATION.get() ? anim : (isSelected ? 1.0f : 0.0f);
        float textExpand = isSelected && Config.ENABLE_TEXT_HOVER_ANIMATION.get() ? anim : 0.0f;

        int insetX = Math.round(Mth.lerp(worldExpand, 4.0f, 0.0f));
        int insetY = Math.round(Mth.lerp(worldExpand, 4.0f, 0.0f));

        int itemLeft = left + insetX;
        int itemRight = left + effectiveWidth - insetX;
        int itemTop = top + insetY;
        int itemBottom = top + height - insetY;
        int itemWidth = itemRight - itemLeft;
        int itemHeight = itemBottom - itemTop;

        if (hovered && !isSelected) {
            graphics.fill(itemLeft - 1, itemTop - 3, itemRight + 1, itemBottom + 3, 0x30FFFFFF);
        }

        if (anim > 0.0f) {
            int selectedLeft = itemLeft - 1;
            int selectedRight = itemRight + 1;
            int selectedTop = itemTop + 1;
            int selectedBottom = itemBottom - 1;
            int alpha = Math.round(0xA0 * anim);
            graphics.fill(selectedLeft, selectedTop, selectedRight, selectedBottom, alpha << 24);
        }

        int iconSize = Math.round(Mth.lerp(worldExpand, 24.0f, 32.0f));
        int iconX = itemLeft + 4;
        int iconY = itemTop + (itemHeight - iconSize) / 2;

        if (Minecraft.getInstance().screen instanceof ReimaginedSelectWorldScreen screen) {
            graphics.blit(screen.loadIcon(s), iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        }

        float textScaleBoost = Mth.lerp(textExpand, 0.88f, 1.0f);
        int textBaseX = itemLeft + 12 + iconSize;
        int maxW = itemRight - textBaseX - 8;

        int tw = f.width(name);
        float fitScale = tw > maxW ? (float) maxW / tw : 1.0f;
        float finalScale = fitScale * textScaleBoost;

        graphics.pose().pushPose();
        float textY = itemTop + (itemHeight - (f.lineHeight * finalScale)) / 2f;
        graphics.pose().translate(textBaseX, textY, 0);
        graphics.pose().scale(finalScale, finalScale, 1.0f);
        graphics.drawString(f, name, 0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        WorldSelectionList.WorldListEntry entry = (WorldSelectionList.WorldListEntry) (Object) this;
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