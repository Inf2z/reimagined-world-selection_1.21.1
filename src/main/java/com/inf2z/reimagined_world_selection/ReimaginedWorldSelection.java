package com.inf2z.reimagined_world_selection;

import com.inf2z.reimagined_world_selection.screen.CustomLinesScreen;
import com.inf2z.reimagined_world_selection.screen.PanelOrderScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import javax.annotation.Nonnull;

@Mod(value = "reimagined_world_selection", dist = Dist.CLIENT)
public class ReimaginedWorldSelection {

    private static ModContainer savedContainer;

    public ReimaginedWorldSelection(ModContainer container) {
        savedContainer = container;

        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, parent) -> new ConfigMenuScreen(parent));
    }

    private static class ConfigMenuScreen extends Screen {
        private final Screen parent;
        private static final int BUTTON_WIDTH = 200;
        private static final int BUTTON_HEIGHT = 20;
        private static final int SPACING = 5;

        public ConfigMenuScreen(Screen parent) {
            super(Component.literal("Reimagined World Selection Config"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int startY = 50;

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal("Main Config"),
                            btn -> {
                                if (this.minecraft != null) {
                                    this.minecraft.setScreen(new ConfigurationScreen(savedContainer, this.parent));
                                }
                            }
                    ).bounds(this.width / 2 - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            );

            startY += BUTTON_HEIGHT + SPACING;

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal("Custom Lines Editor"),
                            btn -> {
                                if (this.minecraft != null) {
                                    this.minecraft.setScreen(new CustomLinesScreen(this.parent));
                                }
                            }
                    ).bounds(this.width / 2 - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            );

            startY += BUTTON_HEIGHT + SPACING;

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal("Panel Order Editor"),
                            btn -> {
                                if (this.minecraft != null) {
                                    this.minecraft.setScreen(new PanelOrderScreen(this.parent));
                                }
                            }
                    ).bounds(this.width / 2 - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            );

            this.addRenderableWidget(
                    Button.builder(
                            Component.translatable("gui.done"),
                            btn -> this.onClose()
                    ).bounds(this.width / 2 - BUTTON_WIDTH / 2, this.height - 25, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            );
        }

        @Override
        public void render(@Nonnull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            super.render(gui, mouseX, mouseY, partialTick);

            // Верхний разделитель (на 2px ниже = 36)
            gui.fill(0, 36, this.width, 37, 0x44FFFFFF);

            // Нижний разделитель (на 7px выше = height-42)
            gui.fill(0, this.height - 42, this.width, this.height - 41, 0x44FFFFFF);

            gui.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        }

        @Override
        public void onClose() {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }
    }
}