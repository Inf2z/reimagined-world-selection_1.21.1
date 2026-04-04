package com.inf2z.reimagined_world_selection.screen;

import com.inf2z.reimagined_world_selection.Config;
import com.inf2z.reimagined_world_selection.mixin.WorldListEntryAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

@ParametersAreNonnullByDefault
public class ReimaginedSelectWorldScreen extends SelectWorldScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReimaginedSelectWorldScreen.class);
    private int panelWidth;
    private WorldSelectionList cachedList;
    private final Map<String, ResourceLocation> iconCache = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private static final ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_server.png");

    public ReimaginedSelectWorldScreen(@Nullable Screen lastScreen) {
        super(lastScreen);
        hideVanillaTitle();
    }

    private void hideVanillaTitle() {
        try {
            Field titleField = Screen.class.getDeclaredField("title");
            titleField.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(titleField, titleField.getModifiers() & ~Modifier.FINAL);
            titleField.set(this, Component.empty());
        } catch (Exception e) {
            LOGGER.error("Failed to hide vanilla title", e);
        }
    }

    @Override
    protected void init() {
        super.init();
        applyReimaginedLayout();
    }

    @Override
    @Nonnull
    public Component getTitle() {
        return Component.empty();
    }

    private void applyReimaginedLayout() {
        this.panelWidth = (int) (this.width * Config.PANEL_WIDTH_RATIO.get());
        int rx = this.panelWidth;
        int rw = this.width - this.panelWidth;
        List<AbstractWidget> footerWidgets = new ArrayList<>();

        for (GuiEventListener listener : this.children()) {
            if (listener instanceof AbstractWidget widget) {
                if (widget.getY() < 32 && !(widget instanceof EditBox)) {
                    widget.visible = false;
                    widget.active = false;
                    widget.setX(-10000);
                    continue;
                }
                if (widget instanceof WorldSelectionList list) {
                    this.cachedList = list;
                    list.setWidth(rw - 40);
                    list.setX(rx + 20);
                } else if (widget instanceof EditBox search) {
                    search.setY(22);
                    search.setX(this.panelWidth + (rw - search.getWidth()) / 2);
                } else if (widget.getY() > this.height - 80) {
                    footerWidgets.add(widget);
                }
            }
        }
        scaleAndPositionButtons(footerWidgets, rx, rw);
    }

    private void scaleAndPositionButtons(List<AbstractWidget> buttons, int rx, int rw) {
        if (buttons.isEmpty()) return;
        Map<Integer, List<AbstractWidget>> rows = new TreeMap<>();
        for (AbstractWidget w : buttons) rows.computeIfAbsent((w.getY() / 10) * 10, k -> new ArrayList<>()).add(w);
        for (List<AbstractWidget> row : rows.values()) {
            row.sort(Comparator.comparingInt(AbstractWidget::getX));
            int spacing = 4;
            int totalSpacing = spacing * (row.size() - 1);
            int maxAvailableWidth = rw - 40 - totalSpacing;
            int originalWidth = 0;
            for (AbstractWidget w : row) originalWidth += w.getWidth();
            float scale = originalWidth > maxAvailableWidth ? (float) maxAvailableWidth / originalWidth : 1.0f;
            int currentX = rx + (rw - (int)(originalWidth * scale) - totalSpacing) / 2;
            for (AbstractWidget w : row) {
                w.setX(currentX);
                w.setWidth((int) (w.getWidth() * scale));
                currentX += w.getWidth() + spacing;
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float tick) {
        int originalWidth = this.width;
        int rw = this.width - this.panelWidth;

        int desiredCenter = this.panelWidth + rw / 2;

        this.width = desiredCenter * 2;

        super.render(gui, mouseX, mouseY, tick);

        this.width = originalWidth;

        int alpha = 102;
        int bgColor = (Config.PANEL_BACKGROUND_STYLE.get() == Config.BackgroundStyle.GRAY) ? 0x1A1A1A : 0x000000;

        gui.fill(0, 0, this.panelWidth, this.height, (alpha << 24) | bgColor);
        gui.fill(this.panelWidth - 1, 0, this.panelWidth, this.height, 0x66555555);

        renderSidebarInfo(gui);
    }

    private void renderSidebarInfo(GuiGraphics gui) {
        if (this.cachedList == null || !(this.cachedList.getSelected() instanceof WorldSelectionList.WorldListEntry entry)) return;
        LevelSummary s = ((WorldListEntryAccessor) (Object) entry).getSummary();
        int cx = this.panelWidth / 2;
        int y = 20;
        int pW = this.panelWidth - 20;
        int iconHeight = pW * 9 / 16;
        gui.blit(loadIcon(s), cx - pW / 2, y, 0, 0, pW, iconHeight, pW, iconHeight);
        y += iconHeight + 12;
        y = drawScaled(gui, s.getLevelName(), cx, y, 0xFFFFFF, pW, true, false);
        y += 2;
        y = drawScaled(gui, "(" + s.getLevelId() + ")", cx, y, 0x777777, pW, false, true);
        y += 12;
        String modeText = s.isHardcore() ? "Hardcore!" : s.getGameMode().getName().substring(0, 1).toUpperCase() + s.getGameMode().getName().substring(1);
        int modeColor = s.isHardcore() ? 0xFFFF0000 : 0xAAAAAA;
        renderLine(gui, "Mode: ", modeText, cx, y, modeColor, pW);
        y += 12;
        renderLine(gui, "Version: ", s.levelVersion().minecraftVersionName(), cx, y, 0xAAAAAA, pW);
        y += 12;
        Integer redColorObj = ChatFormatting.RED.getColor();
        int cheatsColor = (redColorObj != null && s.hasCommands()) ? redColorObj : 0xAAAAAA;
        renderLine(gui, "Commands: ", s.hasCommands() ? "ON" : "OFF", cx, y, cheatsColor, pW);
        y += 15;
        String dateStr = "(" + dateFormat.format(new Date(s.getLastPlayed())) + ")";
        drawScaled(gui, dateStr, cx, y, 0x777777, pW, false, true);
    }

    private int drawScaled(GuiGraphics g, String text, int x, int y, int color, int maxWidth, boolean bold, boolean italic) {
        Style st = Style.EMPTY.withBold(bold).withItalic(italic);
        FormattedCharSequence fcs = Component.literal(text).withStyle(st).getVisualOrderText();
        float tw = this.font.width(fcs);
        float scale = tw > maxWidth ? (float) maxWidth / tw : 1.0f;
        g.pose().pushPose();
        g.pose().translate(x, y + (this.font.lineHeight * (1.0f - scale) / 2.0f), 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(this.font, fcs, (int)(-tw / 2), 0, color, true);
        g.pose().popPose();
        return y + (int)(this.font.lineHeight * scale) + 2;
    }

    private void renderLine(GuiGraphics g, String label, String value, int cx, int y, int valueColor, int maxWidth) {
        String full = label + value;
        float tw = this.font.width(full);
        float scale = tw > maxWidth ? (float) maxWidth / tw : 1.0f;
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        int startX = (int)(-tw / 2);
        g.drawString(this.font, label, startX, 0, 0x555555, false);
        g.drawString(this.font, value, startX + this.font.width(label), 0, valueColor, false);
        g.pose().popPose();
    }

    @Nonnull
    public ResourceLocation loadIcon(LevelSummary s) {
        String id = s.getLevelId();
        if (iconCache.containsKey(id)) return iconCache.get(id);
        Path p = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(id).resolve("icon.png");
        if (Files.exists(p)) {
            try (InputStream is = Files.newInputStream(p)) {
                DynamicTexture dt = new DynamicTexture(NativeImage.read(is));
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("reimagined", "icon_" + id.toLowerCase().replaceAll("[^a-z0-9_]", "_"));
                Minecraft.getInstance().getTextureManager().register(rl, dt);
                iconCache.put(id, rl);
                return rl;
            } catch (Exception ignored) {}
        }
        return DEFAULT_ICON;
    }

    @Override
    public void onClose() {
        iconCache.values().forEach(loc -> { if (!loc.equals(DEFAULT_ICON)) Minecraft.getInstance().getTextureManager().release(loc); });
        iconCache.clear();
        super.onClose();
    }
}