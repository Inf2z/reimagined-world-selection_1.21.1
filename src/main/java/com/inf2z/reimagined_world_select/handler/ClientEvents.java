package com.inf2z.reimagined_world_select.handler;

import com.inf2z.reimagined_world_select.screen.ReimaginedSelectWorldScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = "reimagined_world_select", value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getScreen().getClass() == SelectWorldScreen.class) {
            Minecraft mc = Minecraft.getInstance();
            Screen parent = mc.screen != null ? mc.screen : new TitleScreen();
            event.setNewScreen(new ReimaginedSelectWorldScreen(parent));
        }
    }
}