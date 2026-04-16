package com.inf2z.reimagined_world_selection.mixin;

import com.inf2z.reimagined_world_selection.Config;
import com.inf2z.reimagined_world_selection.screen.ReimaginedSelectWorldScreen;
import com.inf2z.reimagined_world_selection.util.MultiSelectableList;
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
    private void rws$onRender(GuiGraphics graphics, int index, int top, int left, int width, int height,
                              int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        ci.cancel();

        WorldSelectionList.WorldListEntry self = (WorldSelectionList.WorldListEntry) (Object) this;
        WorldListEntryAccessor acc = (WorldListEntryAccessor) this;
        LevelSummary s = acc.getSummary();
        WorldSelectionList parentList = acc.getList();
        Font f = Minecraft.getInstance().font;

        String name = s.getLevelName().isEmpty() ? s.getLevelId() : s.getLevelName();
        boolean isSelected = parentList.getSelected() == self;
        boolean isMultiSelected = parentList instanceof MultiSelectableList multiList && multiList.rws$isMultiSelected(self);

        float anim = reimagined$selectAnimation.getOrDefault(self, 0.0f);
        float target = (isSelected || isMultiSelected) ? 1.0f : 0.0f;

        if (Config.ENABLE_WORLD_HOVER_ANIMATION.get()) {
            float lerpSpeed = (target > anim) ? 0.14f : 0.45f;
            anim = Mth.lerp(lerpSpeed, anim, target);
            if (Math.abs(anim - target) < 0.01f) anim = target;
        } else {
            anim = target;
        }
        reimagined$selectAnimation.put(self, anim);

        int staticLeft = left - 2;
        int staticRight = left + width + 2;
        int staticTop = top - 2;
        int staticBottom = top + height + 2;

        if (hovered && anim < 1.0f) {
            graphics.fill(staticLeft, staticTop, staticRight, staticBottom, 0x20FFFFFF);
        }

        if (anim > 0.0f) {
            int alpha = Math.round(0xA0 * anim);
            graphics.fill(staticLeft, staticTop, staticRight, staticBottom, alpha << 24);

            int borderAlpha = Math.round(255 * anim);
            int color = (borderAlpha << 24) | 0xFFFFFF;

            graphics.fill(staticLeft, staticTop, staticRight, staticTop + 1, color);
            graphics.fill(staticLeft, staticBottom - 1, staticRight, staticBottom, color);
            graphics.fill(staticLeft, staticTop, staticLeft + 1, staticBottom, color);
            graphics.fill(staticRight - 1, staticTop, staticRight, staticBottom, color);
        }

        float iconAnim = Config.ENABLE_WORLD_HOVER_ANIMATION.get() ? anim : target;
        float textScale = Config.ENABLE_TEXT_HOVER_ANIMATION.get() ? Mth.lerp(anim, 0.88f, 1.0f) : 1.0f;

        int insetX = Math.round(Mth.lerp(iconAnim, 4.0f, 0.0f));
        int iconSize = Math.round(Mth.lerp(iconAnim, 24.0f, 32.0f));
        int iconX = left + insetX + 4;
        int iconY = top + (height - iconSize) / 2;

        if (Minecraft.getInstance().screen instanceof ReimaginedSelectWorldScreen screen) {
            graphics.blit(screen.loadIcon(s), iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        }

        int textBaseX = left + insetX + 12 + iconSize;
        graphics.pose().pushPose();
        float textY = top + (height - (f.lineHeight * textScale)) / 2f;
        graphics.pose().translate(textBaseX, textY, 0);
        graphics.pose().scale(textScale, textScale, 1.0f);
        graphics.drawString(f, name, 0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rws$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
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