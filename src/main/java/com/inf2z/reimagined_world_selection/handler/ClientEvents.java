package com.inf2z.reimagined_world_selection.handler;

import com.inf2z.reimagined_world_selection.screen.ReimaginedSelectWorldScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public class ClientEvents {
    @SubscribeEvent
    public void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getScreen().getClass() == SelectWorldScreen.class) {
            Minecraft mc = Minecraft.getInstance();
            Screen current = mc.screen;
            Screen parent = (current != null) ? current : event.getScreen();
            event.setNewScreen(new ReimaginedSelectWorldScreen(parent));
        }
    }
}