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
    private final Map<String, ResourceLocation> iconCache = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private static final ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_server.png");

    public ReimaginedSelectWorldScreen(@Nullable Screen lastScreen) {
        super(lastScreen);
    }

    @Override
    protected void init() {
        super.init();
        this.panelWidth = (int) (this.width * Config.PANEL_WIDTH_RATIO.get());

        SelectWorldAccessor acc = (SelectWorldAccessor) this;
        int rx = this.panelWidth;
        int rw = this.width - this.panelWidth;

        EditBox search = acc.getSearchBox();
        if (search != null) {
            search.setWidth(160);
            search.setX(rx + (rw - 160) / 2);
            search.setY(22);
        }

        WorldSelectionList list = acc.getList();
        if (list != null) {
            list.setRectangle(rw - 40, this.height - 110, rx + 20, 48);
        }

        repositionButtons(rx, rw);
    }

    private void repositionButtons(int rx, int rw) {
        List<AbstractWidget> top = new ArrayList<>();
        List<AbstractWidget> bottom = new ArrayList<>();
        for (Renderable r : this.renderables) {
            if (r instanceof AbstractWidget w && !(w instanceof EditBox)) {
                if (w.getY() < 48) top.add(w);
                else if (w.getY() > this.height - 55) bottom.add(w);
            }
        }
        alignButtons(top, rx, rw);
        alignButtons(bottom, rx, rw);
    }

    private void alignButtons(List<AbstractWidget> buttons, int rx, int rw) {
        if (buttons.isEmpty()) return;
        buttons.sort(Comparator.comparingInt(AbstractWidget::getX));
        int totalWidth = 0;
        for (AbstractWidget b : buttons) totalWidth += b.getWidth() + 4;

        int currentX = rx + (rw - (totalWidth - 4)) / 2;
        for (AbstractWidget b : buttons) {
            b.setX(currentX);
            currentX += b.getWidth() + 4;
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics gui, int mouseX, int mouseY, float tick) {
        this.renderBackground(gui, mouseX, mouseY, tick);

        SelectWorldAccessor acc = (SelectWorldAccessor) this;
        // Рендерим список отдельно, если он есть
        if (acc.getList() != null) acc.getList().render(gui, mouseX, mouseY, tick);

        for (Renderable r : this.renderables) {
            if (!(r instanceof WorldSelectionList)) r.render(gui, mouseX, mouseY, tick);
        }
        renderCustomUI(gui);
    }

    private void renderCustomUI(GuiGraphics gui) {
        int color = (Config.PANEL_BACKGROUND_STYLE.get() == Config.BackgroundStyle.GRAY) ? 0x1A1A1A : 0x000000;
        gui.fill(0, 0, this.panelWidth, this.height, (0xFF << 24) | color);
        gui.fill(this.panelWidth - 1, 0, this.panelWidth, this.height, 0xFF555555);

        gui.drawCenteredString(this.font, this.title, this.panelWidth + (this.width - this.panelWidth) / 2, 8, 0xFFFFFF);

        renderSidebarInfo(gui);
    }

    private void renderSidebarInfo(GuiGraphics gui) {
        WorldSelectionList list = ((SelectWorldAccessor) this).getList();
        if (list == null || !(list.getSelected() instanceof WorldSelectionList.WorldListEntry entry)) return;

        LevelSummary s = ((EntryAccessor) (Object) entry).getSummary();
        int cx = this.panelWidth / 2;
        int y = 40;
        int pW = this.panelWidth - 20;

        int iconHeight = pW * 9 / 16;
        gui.blit(loadIcon(s), cx - pW / 2, y, 0, 0, pW, iconHeight, pW, iconHeight);

        y += iconHeight + 15;
        y = drawWrapped(gui, s.getLevelName(), cx, y, 0xFFFFFF, true);
        y += 5;
        y = drawWrapped(gui, "(" + s.getLevelId() + ")", cx, y, 0x555555, false);
        y += 15;

        renderLine(gui, "Mode: ", s.getGameMode().getName(), cx, y);
        y += 12;
        renderLine(gui, "Version: ", s.levelVersion().minecraftVersionName(), cx, y);
        y += 15;

        drawWrapped(gui, "(" + dateFormat.format(new Date(s.getLastPlayed())) + ")", cx, y, 0x555555, false);
    }

    public ResourceLocation loadIcon(LevelSummary s) {
        String id = s.getLevelId();
        if (iconCache.containsKey(id)) return iconCache.get(id);

        Path p = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(id).resolve("icon.png");
        if (Files.exists(p)) {
            try (InputStream is = Files.newInputStream(p)) {
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

    private int drawWrapped(GuiGraphics g, String text, int x, int y, int color, boolean bold) {
        Style st = Style.EMPTY.withBold(bold).withItalic(!bold && text.startsWith("("));
        List<FormattedCharSequence> lines = this.font.split(Component.literal(text).withStyle(st), this.panelWidth - 20);
        for (FormattedCharSequence l : lines) {
            g.drawCenteredString(this.font, l, x, y, color);
            y += 10;
        }
        return y;
    }

    private void renderLine(GuiGraphics g, String label, String value, int cx, int y) {
        int tw = this.font.width(label) + this.font.width(value);
        g.drawString(this.font, label, cx - tw / 2, y, 0x555555, false);
        g.drawString(this.font, value, cx - tw / 2 + this.font.width(label), y, 0xAAAAAA, false);
    }
}