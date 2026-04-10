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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PanelOrderScreen extends Screen {
    private final Screen parent;
    private PanelElementsList elementsList;

    public PanelOrderScreen(Screen parent) {
        super(Component.translatable("screen.reimagined_world_selection.panel_order_editor"));
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
                Button.builder(Component.translatable("screen.reimagined_world_selection.reset"), btn -> resetOrder())
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
        gui.drawCenteredString(this.font, Component.translatable("screen.reimagined_world_selection.drag_to_reorder"),
                this.width / 2, 22, 0x808080);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private class PanelElementsList extends ObjectSelectionList<PanelElementEntry> {
        private static final int TOP_Y = 33;
        private static final int BOTTOM_OFFSET = 33;

        private PanelElementEntry dragging;
        private long dragStartTime;
        private final int bottomLimit;

        public PanelElementsList(Minecraft minecraft) {
            super(minecraft,
                    PanelOrderScreen.this.width,
                    PanelOrderScreen.this.height - BOTTOM_OFFSET - TOP_Y,
                    TOP_Y,
                    28);
            this.bottomLimit = PanelOrderScreen.this.height - BOTTOM_OFFSET;
            reload();
        }

        public void reload() {
            this.clearEntries();

            List<? extends String> savedOrder = Config.PANEL_ORDER.get();
            List<Config.CustomLineConfig> customLines = Config.getCustomLines();

            Set<String> addedIds = new HashSet<>();

            for (String id : savedOrder) {
                if (id.startsWith("custom_")) {
                    try {
                        int customIndex = Integer.parseInt(id.substring(7));
                        if (customIndex >= 0 && customIndex < customLines.size()) {
                            Config.CustomLineConfig line = customLines.get(customIndex);
                            this.addEntry(new PanelElementEntry(id, Component.translatable("screen.reimagined_world_selection.custom_prefix", line.label()).getString(), null));
                            addedIds.add(id);
                        }
                    } catch (NumberFormatException ignored) {}
                } else {
                    PanelElementEntry entry = createEntryFromId(id);
                    if (entry != null) {
                        this.addEntry(entry);
                        addedIds.add(id);
                    }
                }
            }

            for (String defaultId : getDefaultOrder()) {
                if (!addedIds.contains(defaultId)) {
                    PanelElementEntry entry = createEntryFromId(defaultId);
                    if (entry != null) {
                        this.addEntry(entry);
                        addedIds.add(defaultId);
                    }
                }
            }

            for (int i = 0; i < customLines.size(); i++) {
                String customId = "custom_" + i;
                if (!addedIds.contains(customId)) {
                    Config.CustomLineConfig line = customLines.get(i);
                    this.addEntry(new PanelElementEntry(customId, Component.translatable("screen.reimagined_world_selection.custom_prefix", line.label()).getString(), null));
                }
            }
        }

        private PanelElementEntry createEntryFromId(String id) {
            return switch (id) {
                case "large_icon" -> new PanelElementEntry(id, Component.translatable("info_order.reimagined_world_selection.large_icon").getString(), Config.SHOW_LARGE_ICON);
                case "world_name" -> new PanelElementEntry(id, Component.translatable("info_order.reimagined_world_selection.world_name").getString(), Config.SHOW_WORLD_NAME);
                case "folder_name" -> new PanelElementEntry(id, Component.translatable("info_order.reimagined_world_selection.folder_name").getString(), Config.SHOW_FOLDER_NAME);
                case "game_mode" -> new PanelElementEntry(id, Component.translatable("info_order.reimagined_world_selection.game_mode").getString(), Config.SHOW_GAME_MODE);
                case "difficulty" -> new PanelElementEntry(id, Component.translatable("info_order.reimagined_world_selection.difficulty").getString(), Config.SHOW_DIFFICULTY);
                case "time_played" -> new PanelElementEntry(id, Component.translatable("info_order.reimagined_world_selection.time_played").getString(), Config.SHOW_TIME_PLAYED);
                case "version" -> new PanelElementEntry(id, Component.translatable("info_order.reimagined_world_selection.version").getString(), Config.SHOW_VERSION);
                case "cheats" -> new PanelElementEntry(id, Component.translatable("info_order.reimagined_world_selection.cheats").getString(), Config.SHOW_CHEATS);
                case "last_played" -> new PanelElementEntry(id, Component.translatable("info_order.reimagined_world_selection.last_played").getString(), Config.SHOW_LAST_PLAYED);
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

        public boolean isDragging(PanelElementEntry entry) {
            return dragging == entry;
        }

        public long getDragStartTime() {
            return dragStartTime;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseY >= this.getY() && mouseY < this.getBottom() && mouseX >= 0 && mouseX < this.width;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseY >= bottomLimit) {
                return false;
            }
            if (button == 0) {
                PanelElementEntry clicked = this.getEntryAtPosition(mouseX, mouseY);
                if (clicked != null) {
                    dragging = clicked;
                    dragStartTime = System.currentTimeMillis();
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
                newOrder.add(entry.id);
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

            boolean isDragging = PanelOrderScreen.this.elementsList.isDragging(this);

            float targetAnimation = (hovered || isDragging) ? 1.0f : 0.0f;
            hoverAnimation = Mth.lerp(0.3f, hoverAnimation, targetAnimation);

            float scale = 1.0f + (hoverAnimation * 0.05f);

            float shakeX = 0;
            float shakeY = 0;
            if (isDragging) {
                long elapsed = System.currentTimeMillis() - PanelOrderScreen.this.elementsList.getDragStartTime();
                double t = elapsed * 0.008;
                shakeX = (float) (Math.sin(t * 2) * 1.5);
                shakeY = (float) (Math.sin(t) * 0.75);
            }

            int highlightPadding = 6;
            int highlightLeft = left - highlightPadding;
            int highlightRight = left + width + highlightPadding;

            if (hovered || isDragging) {
                int highlightAlpha = (int) (hoverAnimation * 80);
                gui.fill(
                        (int)(highlightLeft + shakeX), (int)(top - 2 + shakeY),
                        (int)(highlightRight + shakeX), (int)(top + height + 2 + shakeY),
                        (highlightAlpha << 24) | 0xFFFFFF
                );
            }

            gui.pose().pushPose();

            gui.pose().translate(shakeX, shakeY, 0);

            gui.pose().translate(left + width / 2f, top + height / 2f, 0);
            gui.pose().scale(scale, scale, 1.0f);
            gui.pose().translate(-(left + width / 2f), -(top + height / 2f), 0);

            boolean enabled = configValue == null || configValue.get();
            int textColor = enabled ? 0xFFFFFF : 0x808080;

            if (isDragging) {
                textColor = 0xFFFF00;
            }

            gui.drawString(PanelOrderScreen.this.font, "≡", left + 5, top + 8, isDragging ? 0xFFFF00 : 0x808080);
            gui.drawString(PanelOrderScreen.this.font, name, left + 20, top + 8, textColor);

            if (configValue != null) {
                String status = enabled ? Component.translatable("screen.reimagined_world_selection.visible").getString() : Component.translatable("screen.reimagined_world_selection.hidden").getString();
                int statusColor = enabled ? 0x55FF55 : 0xFF5555;
                if (isDragging) {
                    statusColor = enabled ? 0xAAFF55 : 0xFFAA55;
                }
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