package com.inf2z.reimagined_world_selection.mixin;

import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;

@SuppressWarnings("rawtypes")
@Mixin(WorldSelectionList.class)
public abstract class WorldSelectionListMixin extends AbstractSelectionList {

    protected WorldSelectionListMixin(net.minecraft.client.Minecraft mc, int width, int height, int top, int itemHeight) {
        super(mc, width, height, top, itemHeight);
    }

    @Override
    public int getRowWidth() {
        int availableWidth = this.width - 40;
        int maxWidth = 400;
        return Math.min(availableWidth, maxWidth);
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.width - 6;
    }
}