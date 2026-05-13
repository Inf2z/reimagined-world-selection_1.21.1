package com.inf2z.reimagined_world_select.mixin;

import com.inf2z.reimagined_world_select.Config;
import com.inf2z.reimagined_world_select.util.CreateWorldCompat;
import com.inf2z.reimagined_world_select.util.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("rawtypes")
@Mixin(WorldSelectionList.class)
public abstract class WorldSelectionListMixin extends AbstractSelectionList {

    protected WorldSelectionListMixin(Minecraft mc, int width, int height, int top, int itemHeight) {
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

    @Redirect(
            method = "loadLevels",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;openFresh(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;)V"
            )
    )
    private void rws$interceptNoWorldsBehaviour(Minecraft mc, Screen lastScreen) {
        if (Config.NO_WORLDS_BEHAVIOUR.get() == Config.NoWorldsBehaviour.GENERATE) {
            MinecraftCompat.runOnMainThread(mc, () -> CreateWorldCompat.openFresh(mc, lastScreen));
        }
    }
}