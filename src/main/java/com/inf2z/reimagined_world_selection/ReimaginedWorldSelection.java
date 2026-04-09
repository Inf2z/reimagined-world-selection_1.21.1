package com.inf2z.reimagined_world_selection;

import com.inf2z.reimagined_world_selection.screen.CustomLinesScreen;
import com.inf2z.reimagined_world_selection.screen.PanelOrderScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.NotNull;

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

        public ConfigMenuScreen(Screen parent) {
            super(Component.literal("Reimagined World Selection Config"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            DummyList dummyList = new DummyList();
            this.addRenderableWidget(dummyList);

            int centerX = this.width / 2 - BUTTON_WIDTH / 2;
            int startY = 40;

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal("Main Config"),
                            btn -> {
                                if (this.minecraft != null) {
                                    this.minecraft.setScreen(new ConfigurationScreen(savedContainer, this));
                                }
                            }
                    ).bounds(centerX, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            );

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal("Custom Info Editor"),
                            btn -> {
                                if (this.minecraft != null) {
                                    this.minecraft.setScreen(new CustomLinesScreen(this));
                                }
                            }
                    ).bounds(centerX, startY + 25, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            );

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal("Info Order Editor"),
                            btn -> {
                                if (this.minecraft != null) {
                                    this.minecraft.setScreen(new PanelOrderScreen(this));
                                }
                            }
                    ).bounds(centerX, startY + 50, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            );

            this.addRenderableWidget(
                    Button.builder(
                            Component.translatable("gui.done"),
                            btn -> this.onClose()
                    ).bounds(this.width / 2 - BUTTON_WIDTH / 2, this.height - 25, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            );
        }

        @Override
        public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            super.render(gui, mouseX, mouseY, partialTick);
            gui.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        }

        @Override
        public void onClose() {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }

        private class DummyList extends ObjectSelectionList<DummyList.DummyEntry> {
            public DummyList() {
                super(ConfigMenuScreen.this.minecraft,
                        ConfigMenuScreen.this.width,
                        ConfigMenuScreen.this.height - 61,
                        32,
                        20);
            }

            @Override
            public int getRowWidth() {
                return 0;
            }

            @Override
            protected int getScrollbarPosition() {
                return this.width + 100;
            }

            private static class DummyEntry extends ObjectSelectionList.Entry<DummyEntry> {
                @Override
                public void render(@Nonnull GuiGraphics gui, int index, int top, int left, int width, int height,
                                   int mouseX, int mouseY, boolean hovered, float partialTick) {
                }

                @Nonnull
                @Override
                public Component getNarration() {
                    return Component.empty();
                }
            }
        }
    }
}