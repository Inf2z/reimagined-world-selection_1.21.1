package com.inf2z.reimagined_world_selection.screen;

import com.inf2z.reimagined_world_selection.Config;
import com.inf2z.reimagined_world_selection.mixin.EntryAccessor;
import com.inf2z.reimagined_world_selection.mixin.SelectWorldAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.storage.LevelSummary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReimaginedSelectWorldScreen extends SelectWorldScreen {
    private int panelWidth;
    private EditBox vanillaSearchBox;
    private static final ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_server.png");
    private final Map<String, ResourceLocation> iconCache = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public ReimaginedSelectWorldScreen(@Nullable Screen lastScreen) {
        super(lastScreen == null ? new TitleScreen() : lastScreen);
    }

    @Override
    protected void init() {
        super.init();
        this.panelWidth = (int) (this.width * Config.PANEL_WIDTH_RATIO.get());

        for (Renderable r : this.renderables) {
            if (r instanceof EditBox editBox) {
                this.vanillaSearchBox = editBox;
                break;
            }
        }
        applyReimaginedLayout();
    }

    @Override
    public void repositionElements() {
        super.repositionElements();
        this.panelWidth = (int) (this.width * Config.PANEL_WIDTH_RATIO.get());
        applyReimaginedLayout();
    }

    private void applyReimaginedLayout() {
        int rightAreaStart = this.panelWidth;
        int rightAreaWidth = this.width - this.panelWidth;
        int rightCenterX = rightAreaStart + (rightAreaWidth / 2);

        // Исправляем список
        WorldSelectionList list = ((SelectWorldAccessor) this).getList();
        if (list != null) {
            int padding = 16;
            list.setWidth(rightAreaWidth - (padding * 2));
            list.setX(rightAreaStart + padding);
        }

        if (this.vanillaSearchBox != null) {
            this.vanillaSearchBox.setWidth(Math.min(160, rightAreaWidth - 40));
            this.vanillaSearchBox.setX(rightCenterX - this.vanillaSearchBox.getWidth() / 2);
        }

        // Исправляем кнопки (строгая логика)
        List<AbstractWidget> buttons = new ArrayList<>();
        for (Renderable r : this.renderables) {
            if (r instanceof AbstractWidget w && w != this.vanillaSearchBox && !(w instanceof WorldSelectionList)) {
                if (w.getY() > this.height - 64) {
                    buttons.add(w);
                }
            }
        }

        if (!buttons.isEmpty()) {
            buttons.sort(Comparator.comparingInt(AbstractWidget::getX));

            int spacing = 4;
            int availableSpace = rightAreaWidth - 10;

            // Считаем текущую суммарную ширину
            int totalWidth = 0;
            for (AbstractWidget b : buttons) {
                // Возвращаем ванильные пропорции (Play: 150, Edit/Delete: 74)
                int targetW = (b.getMessage().getString().length() > 10) ? 150 : 74;
                b.setWidth(targetW);
                totalWidth += targetW + spacing;
            }
            totalWidth -= spacing;

            // Если не влезает, принудительно сжимаем ВСЕ большие кнопки до 100
            if (totalWidth > availableSpace) {
                totalWidth = 0;
                for (AbstractWidget b : buttons) {
                    if (b.getWidth() > 100) b.setWidth(100);
                    totalWidth += b.getWidth() + spacing;
                }
                totalWidth -= spacing;
            }

            // Если всё равно не влезает, сжимаем пропорционально до минимума
            if (totalWidth > availableSpace) {
                float shrinkFactor = (float) availableSpace / totalWidth;
                totalWidth = 0;
                for (AbstractWidget b : buttons) {
                    b.setWidth((int) (b.getWidth() * shrinkFactor));
                    totalWidth += b.getWidth() + spacing;
                }
                totalWidth -= spacing;
            }

            // Центрируем и расставляем БЕЗ наслоения
            int currentX = rightCenterX - (totalWidth / 2);
            if (currentX < rightAreaStart + 5) currentX = rightAreaStart + 5;

            for (AbstractWidget b : buttons) {
                b.setX(currentX);
                currentX += b.getWidth() + spacing;
            }
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        applyReimaginedLayout();
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Отрисовка инфопанели (поверх списка)
        int alpha = Config.PANEL_OPACITY.get() & 0xFF;
        int bgColor = (Config.PANEL_BACKGROUND_STYLE.get() == Config.BackgroundStyle.GRAY) ? 0x1A1A1A : 0x000000;
        guiGraphics.fill(0, 0, this.panelWidth, this.height, (alpha << 24) | bgColor);
        guiGraphics.fill(this.panelWidth - 1, 0, this.panelWidth, this.height, 0xFF555555);

        renderSidebarContent(guiGraphics);
    }

    private void renderSidebarContent(GuiGraphics guiGraphics) {
        WorldSelectionList list = ((SelectWorldAccessor) this).getList();
        if (list == null || !(list.getSelected() instanceof WorldSelectionList.WorldListEntry worldEntry)) return;

        LevelSummary summary = ((EntryAccessor) (Object) worldEntry).getSummary();
        int centerX = this.panelWidth / 2;
        int maxWidth = this.panelWidth - 20;
        int y = 40;

        // Иконка мира
        guiGraphics.blit(loadWorldIcon(summary), centerX - 46, y, 0, 0, 92, 92, 92, 92);
        y += 105;

        // Название мира (Жирный)
        renderWrappedText(guiGraphics, summary.getLevelName(), centerX, y, maxWidth, 0xFFFFFF, true);
        y += 15;

        // Название папки (Сразу под названием мира)
        if (Config.SHOW_FOLDER_NAME.get()) {
            renderWrappedText(guiGraphics, summary.getLevelId(), centerX, y, maxWidth, 0x666666, false);
            y += 20;
        }

        y += 10;

        // Режим игры (Mode: Survival)
        String rawMode = summary.getGameMode().getName();
        String capMode = rawMode.substring(0, 1).toUpperCase() + rawMode.substring(1);
        int modeColor = summary.isHardcore() ? 0xFF0000 : 0xFFFFFF;
        String modeText = summary.isHardcore() ? "Hardcore" : capMode;

        Component modeLine = Component.translatable("gui.reimagined.mode").append(": ")
                .append(Component.literal(modeText).withStyle(s -> s.withColor(modeColor)));

        guiGraphics.drawCenteredString(this.font, modeLine, centerX, y, 0xAAAAAA);
        y += 15;

        // Версия и дата
        guiGraphics.drawCenteredString(this.font, summary.levelVersion().minecraftVersionName(), centerX, y, 0x888888);
        y += 15;
        String dateStr = dateFormat.format(new Date(summary.getLastPlayed()));
        guiGraphics.drawCenteredString(this.font, dateStr, centerX, y, 0x555555);
    }

    private void renderWrappedText(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color, boolean bold) {
        Style style = bold ? Style.EMPTY.withBold(true) : Style.EMPTY;
        List<FormattedCharSequence> lines = this.font.split(Component.literal(text).withStyle(style), maxWidth);
        int currentY = y;
        for (FormattedCharSequence line : lines) {
            graphics.drawCenteredString(this.font, line, x, currentY, color);
            currentY += 10;
        }
    }

    private ResourceLocation loadWorldIcon(LevelSummary summary) {
        String id = summary.getLevelId();
        if (iconCache.containsKey(id)) return iconCache.get(id);

        Path path = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(id).resolve("icon.png");
        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                NativeImage ni = NativeImage.read(is);
                DynamicTexture dt = new DynamicTexture(ni);
                String safeId = id.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("reimagined", "icon_" + safeId);
                Minecraft.getInstance().getTextureManager().register(rl, dt);
                iconCache.put(id, rl);
                return rl;
            } catch (Exception ignored) {}
        }
        return DEFAULT_ICON;
    }

    @Override
    public void onClose() {
        iconCache.values().forEach(loc -> {
            if (!loc.equals(DEFAULT_ICON)) Minecraft.getInstance().getTextureManager().release(loc);
        });
        iconCache.clear();
        super.onClose();
    }
}