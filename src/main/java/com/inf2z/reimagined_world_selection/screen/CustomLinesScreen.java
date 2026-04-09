package com.inf2z.reimagined_world_selection.screen;

import com.inf2z.reimagined_world_selection.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class CustomLinesScreen extends Screen {
    private final Screen parent;
    private CustomLinesList linesList;

    public CustomLinesScreen(Screen parent) {
        super(Component.literal("Custom Lines Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Minecraft mc = this.minecraft;
        if (mc == null) return;

        this.linesList = new CustomLinesList(mc);
        this.addRenderableWidget(this.linesList);

        int buttonWidth = 150;
        int spacing = 5;
        int totalWidth = buttonWidth * 2 + spacing;
        int startX = (this.width - totalWidth) / 2;

        this.addRenderableWidget(
                Button.builder(Component.literal("+ Add Line"), btn -> addNewLine())
                        .bounds(startX, this.height - 26, buttonWidth, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), btn -> onClose())
                        .bounds(startX + buttonWidth + spacing, this.height - 26, buttonWidth, 20)
                        .build()
        );
    }

    private void addNewLine() {
        List<String> current = new ArrayList<>(Config.CUSTOM_LINES.get());
        current.add("New Label;%var%;VALUE;WORLD");
        Config.CUSTOM_LINES.set(current);
        Config.SPEC.save();
        this.linesList.reload();
    }

    @Override
    public void render(@Nonnull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);

        gui.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        if (this.linesList.children().isEmpty()) {
            gui.drawCenteredString(this.font,
                    Component.literal("No custom lines. Click '+ Add Line' to create one!"),
                    this.width / 2, this.height / 2, 0x808080);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.linesList != null && this.linesList.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.linesList != null && this.linesList.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.linesList != null && this.linesList.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private class CustomLinesList extends ObjectSelectionList<CustomLineEntry> {
        public CustomLinesList(Minecraft minecraft) {
            super(minecraft,
                    CustomLinesScreen.this.width,
                    CustomLinesScreen.this.height - 61,
                    32,
                    76);
            reload();
        }

        public void reload() {
            this.clearEntries();
            List<? extends String> lines = Config.CUSTOM_LINES.get();
            for (int i = 0; i < lines.size(); i++) {
                this.addEntry(new CustomLineEntry(i, lines.get(i)));
            }
        }

        @Override
        public int getRowWidth() {
            return 270;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 145;
        }
    }

    private class CustomLineEntry extends ObjectSelectionList.Entry<CustomLineEntry> {
        private final int index;
        private final EditBox labelBox;
        private final EditBox formatBox;
        private final Button modeButton;
        private final Button sourceButton;
        private final Button deleteButton;

        private Config.VarMode currentMode;
        private Config.VariableSource currentSource;

        public CustomLineEntry(int index, String data) {
            this.index = index;

            String[] parts = data.split(";", -1);
            String label = parts.length > 0 ? parts[0] : "";
            String format = parts.length > 1 ? parts[1] : "";

            this.currentMode = Config.VarMode.VALUE;
            if (parts.length > 2) {
                try {
                    this.currentMode = Config.VarMode.valueOf(parts[2].toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            this.currentSource = Config.VariableSource.WORLD;
            if (parts.length > 3) {
                try {
                    this.currentSource = Config.VariableSource.valueOf(parts[3].toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            this.labelBox = new EditBox(CustomLinesScreen.this.font, 0, 0, 80, 18, Component.literal("Label"));
            this.labelBox.setValue(label);
            this.labelBox.setMaxLength(100);
            this.labelBox.setResponder(s -> saveChanges());

            this.formatBox = new EditBox(CustomLinesScreen.this.font, 0, 0, 80, 18, Component.literal("Format"));
            this.formatBox.setValue(format);
            this.formatBox.setMaxLength(200);
            this.formatBox.setResponder(s -> saveChanges());

            this.modeButton = Button.builder(
                    Component.literal("Mode: " + this.currentMode.name()),
                    btn -> cycleMode()
            ).bounds(0, 0, 75, 18).build();

            this.sourceButton = Button.builder(
                    Component.literal("Src: " + this.currentSource.name()),
                    btn -> cycleSource()
            ).bounds(0, 0, 75, 18).build();

            this.deleteButton = Button.builder(
                    Component.literal("Delete"),
                    btn -> deleteLine()
            ).bounds(0, 0, 75, 18).build();
        }

        private void cycleMode() {
            this.currentMode = switch (this.currentMode) {
                case VALUE -> Config.VarMode.BOOLEAN;
                case BOOLEAN -> Config.VarMode.ON_OFF;
                case ON_OFF -> Config.VarMode.VALUE;
            };
            this.modeButton.setMessage(Component.literal("Mode: " + this.currentMode.name()));
            saveChanges();
        }

        private void cycleSource() {
            this.currentSource = this.currentSource == Config.VariableSource.WORLD
                    ? Config.VariableSource.PLAYER
                    : Config.VariableSource.WORLD;
            this.sourceButton.setMessage(Component.literal("Src: " + this.currentSource.name()));
            saveChanges();
        }

        private void saveChanges() {
            List<String> current = new ArrayList<>(Config.CUSTOM_LINES.get());
            String newValue = labelBox.getValue() + ";" +
                    formatBox.getValue() + ";" +
                    currentMode.name() + ";" +
                    currentSource.name();
            current.set(index, newValue);
            Config.CUSTOM_LINES.set(current);
            Config.SPEC.save();
        }

        private void deleteLine() {
            List<String> current = new ArrayList<>(Config.CUSTOM_LINES.get());
            current.remove(index);
            Config.CUSTOM_LINES.set(current);
            Config.SPEC.save();
            CustomLinesScreen.this.linesList.reload();
        }

        @Override
        public void render(@Nonnull GuiGraphics gui, int entryIdx, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {

            int yOffset = 6;

            gui.drawString(CustomLinesScreen.this.font, "#" + (index + 1), left + 4, top + yOffset + 1, 0xFFFFFF);

            gui.drawString(CustomLinesScreen.this.font, "Label:", left + 20, top + yOffset + 1, 0xAAAAAA);
            this.labelBox.setX(left + 58);
            this.labelBox.setY(top + yOffset);
            this.labelBox.render(gui, mouseX, mouseY, partialTick);

            gui.drawString(CustomLinesScreen.this.font, "Format:", left + 20, top + yOffset + 23, 0xAAAAAA);
            this.formatBox.setX(left + 58);
            this.formatBox.setY(top + yOffset + 22);
            this.formatBox.render(gui, mouseX, mouseY, partialTick);

            int buttonX = left + 188;

            this.modeButton.setX(buttonX);
            this.modeButton.setY(top + yOffset);
            this.modeButton.render(gui, mouseX, mouseY, partialTick);

            this.sourceButton.setX(buttonX);
            this.sourceButton.setY(top + yOffset + 22);
            this.sourceButton.render(gui, mouseX, mouseY, partialTick);

            this.deleteButton.setX(buttonX);
            this.deleteButton.setY(top + yOffset + 44);
            this.deleteButton.render(gui, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.labelBox.mouseClicked(mouseX, mouseY, button)) {
                this.labelBox.setFocused(true);
                this.formatBox.setFocused(false);
                return true;
            }
            if (this.formatBox.mouseClicked(mouseX, mouseY, button)) {
                this.formatBox.setFocused(true);
                this.labelBox.setFocused(false);
                return true;
            }
            if (this.modeButton.mouseClicked(mouseX, mouseY, button)) return true;
            if (this.sourceButton.mouseClicked(mouseX, mouseY, button)) return true;
            if (this.deleteButton.mouseClicked(mouseX, mouseY, button)) return true;

            this.labelBox.setFocused(false);
            this.formatBox.setFocused(false);
            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.labelBox.isFocused() && this.labelBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            return this.formatBox.isFocused() && this.formatBox.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            if (this.labelBox.isFocused() && this.labelBox.charTyped(codePoint, modifiers)) return true;
            return this.formatBox.isFocused() && this.formatBox.charTyped(codePoint, modifiers);
        }

        @Nonnull
        public Component getNarration() {
            return Component.literal("Custom line #" + (index + 1) + ": " + labelBox.getValue());
        }
    }
}