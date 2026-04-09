package com.inf2z.reimagined_world_selection.screen;

import com.inf2z.reimagined_world_selection.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class PanelOrderScreen extends Screen {
    private final Screen parent;
    private PanelElementsList elementsList;

    public PanelOrderScreen(Screen parent) {
        super(Component.literal("Panel Order Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Minecraft mc = this.minecraft;
        if (mc == null) return;

        this.elementsList = new PanelElementsList(mc);
        this.addRenderableWidget(this.elementsList);

        int buttonWidth = 100;
        int spacing = 5;
        int startX = this.width / 2 - (buttonWidth * 2 + spacing) / 2;

        this.addRenderableWidget(
                Button.builder(Component.literal("Reset"), btn -> resetOrder())
                        .bounds(startX, this.height - 26, buttonWidth, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), btn -> onClose())
                        .bounds(startX + buttonWidth + spacing, this.height - 26, buttonWidth, 20)
                        .build()
        );
    }

    private void resetOrder() {
        Config.PANEL_ORDER.set(getDefaultOrder());
        Config.SPEC.save();
        this.elementsList.reload();
    }

    static List<String> getDefaultOrder() {
        List<String> order = new ArrayList<>();
        order.add("large_icon");
        order.add("world_name");
        order.add("folder_name");
        order.add("game_mode");
        order.add("difficulty");
        order.add("time_played");
        order.add("version");
        order.add("cheats");
        order.add("last_played");
        return order;
    }

    @Override
    public void render(@Nonnull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        gui.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        gui.drawCenteredString(this.font, Component.literal("Drag elements to reorder"),
                this.width / 2, 22, 0x808080);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private class PanelElementsList extends ObjectSelectionList<PanelElementEntry> {
        private PanelElementEntry dragging;

        public PanelElementsList(Minecraft minecraft) {
            super(minecraft,
                    PanelOrderScreen.this.width,
                    PanelOrderScreen.this.height - 62,
                    35,
                    28);
            reload();
        }

        public void reload() {
            this.clearEntries();

            List<? extends String> order = Config.PANEL_ORDER.get();
            if (order.isEmpty()) {
                order = getDefaultOrder();
            }

            for (String id : order) {
                PanelElementEntry entry = createEntryFromId(id);
                if (entry != null) {
                    this.addEntry(entry);
                }
            }

            List<Config.CustomLineConfig> customLines = Config.getCustomLines();
            for (int i = 0; i < customLines.size(); i++) {
                Config.CustomLineConfig line = customLines.get(i);
                this.addEntry(new PanelElementEntry("custom_" + i, "Custom: " + line.label(), null));
            }
        }

        private PanelElementEntry createEntryFromId(String id) {
            return switch (id) {
                case "large_icon" -> new PanelElementEntry(id, "Large Icon", Config.SHOW_LARGE_ICON);
                case "world_name" -> new PanelElementEntry(id, "World Name", Config.SHOW_WORLD_NAME);
                case "folder_name" -> new PanelElementEntry(id, "Folder Name", Config.SHOW_FOLDER_NAME);
                case "game_mode" -> new PanelElementEntry(id, "Game Mode", Config.SHOW_GAME_MODE);
                case "difficulty" -> new PanelElementEntry(id, "Difficulty", Config.SHOW_DIFFICULTY);
                case "time_played" -> new PanelElementEntry(id, "Time Played", Config.SHOW_TIME_PLAYED);
                case "version" -> new PanelElementEntry(id, "Version", Config.SHOW_VERSION);
                case "cheats" -> new PanelElementEntry(id, "Commands", Config.SHOW_CHEATS);
                case "last_played" -> new PanelElementEntry(id, "Last Played", Config.SHOW_LAST_PLAYED);
                default -> null;
            };
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 160;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                PanelElementEntry clicked = this.getEntryAtPosition(mouseX, mouseY);
                if (clicked != null) {
                    dragging = clicked;
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (dragging != null && button == 0) {
                int currentIndex = this.children().indexOf(dragging);
                if (currentIndex < 0) {
                    dragging = null;
                    return false;
                }

                double relativeY = mouseY - (double) this.getY() + this.getScrollAmount();
                int targetIndex = Mth.clamp((int) Math.floor(relativeY / this.itemHeight), 0, this.children().size() - 1);

                if (targetIndex != currentIndex) {
                    List<PanelElementEntry> entries = new ArrayList<>(this.children());
                    PanelElementEntry removed = entries.remove(currentIndex);
                    entries.add(targetIndex, removed);

                    this.clearEntries();
                    entries.forEach(this::addEntry);

                    saveOrder();
                }
                return true;
            }
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (dragging != null) {
                dragging = null;
                saveOrder();
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        private void saveOrder() {
            List<String> newOrder = new ArrayList<>();
            for (PanelElementEntry entry : this.children()) {
                if (!entry.id.startsWith("custom_")) {
                    newOrder.add(entry.id);
                }
            }
            Config.PANEL_ORDER.set(newOrder);
            Config.SPEC.save();
        }
    }

    private class PanelElementEntry extends ObjectSelectionList.Entry<PanelElementEntry> {
        private final String id;
        private final String name;
        private final net.neoforged.neoforge.common.ModConfigSpec.BooleanValue configValue;
        private float hoverAnimation = 0.0f;

        public PanelElementEntry(String id, String name, net.neoforged.neoforge.common.ModConfigSpec.BooleanValue configValue) {
            this.id = id;
            this.name = name;
            this.configValue = configValue;
        }

        @Override
        public void render(@Nonnull GuiGraphics gui, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {

            boolean isDragging = PanelOrderScreen.this.elementsList.dragging == this;

            float targetAnimation = (hovered || isDragging) ? 1.0f : 0.0f;
            hoverAnimation = Mth.lerp(0.3f, hoverAnimation, targetAnimation);

            float scale = 1.0f + (hoverAnimation * 0.05f);

            // Белое выделение при hover
            if (hovered || isDragging) {
                int highlightAlpha = (int) (hoverAnimation * 80);
                gui.fill(left, top, left + width, top + height, (highlightAlpha << 24) | 0xFFFFFF);
            }

            gui.pose().pushPose();
            gui.pose().translate(left + width / 2f, top + height / 2f, 0);
            gui.pose().scale(scale, scale, 1.0f);
            gui.pose().translate(-(left + width / 2f), -(top + height / 2f), 0);

            boolean enabled = configValue == null || configValue.get();
            int textColor = enabled ? 0xFFFFFF : 0x808080;

            // Иконка перетаскивания
            gui.drawString(PanelOrderScreen.this.font, "≡", left + 5, top + 8, 0x808080);

            // Название
            gui.drawString(PanelOrderScreen.this.font, name, left + 20, top + 8, textColor);

            // Статус
            if (configValue != null) {
                String status = enabled ? "Visible" : "Hidden";
                int statusColor = enabled ? 0x55FF55 : 0xFF5555;
                int statusWidth = PanelOrderScreen.this.font.width(status);
                gui.drawString(PanelOrderScreen.this.font, status, left + width - statusWidth - 5, top + 8, statusColor);
            }

            gui.pose().popPose();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Nonnull
        public Component getNarration() {
            return Component.literal(name);
        }
    }
}