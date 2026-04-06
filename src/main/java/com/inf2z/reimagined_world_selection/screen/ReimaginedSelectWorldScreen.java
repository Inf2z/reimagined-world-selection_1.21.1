package com.inf2z.reimagined_world_selection.screen;

import com.inf2z.reimagined_world_selection.Config;
import com.inf2z.reimagined_world_selection.api.WorldSelectionAPI;
import com.inf2z.reimagined_world_selection.mixin.WorldListEntryAccessor;
import com.inf2z.reimagined_world_selection.util.WorldInfoHelper;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@ParametersAreNonnullByDefault
public class ReimaginedSelectWorldScreen extends SelectWorldScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReimaginedSelectWorldScreen.class);

    private int panelWidth;
    private WorldSelectionList cachedList;
    private final Map<String, ResourceLocation> iconCache = new HashMap<>();
    private final Map<String, Float> sidebarLineAnimations = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private static final ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_server.png");

    private float sidebarScroll;
    private float sidebarTargetScroll;

    public ReimaginedSelectWorldScreen(@Nullable Screen lastScreen) {
        super(lastScreen);
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
                }
            }
        }

        layoutFooterButtons(rx, rw);
    }

    private void layoutFooterButtons(int rx, int rw) {
        AbstractWidget play = null;
        AbstractWidget create = null;
        AbstractWidget edit = null;
        AbstractWidget delete = null;
        AbstractWidget recreate = null;
        AbstractWidget back = null;

        for (GuiEventListener listener : this.children()) {
            if (listener instanceof AbstractWidget widget && widget.getY() > this.height - 80) {
                String key = getWidgetKey(widget);

                if ("selectWorld.select".equals(key)) {
                    play = widget;
                } else if ("selectWorld.create".equals(key)) {
                    create = widget;
                } else if ("selectWorld.edit".equals(key)) {
                    edit = widget;
                } else if ("selectWorld.delete".equals(key)) {
                    delete = widget;
                } else if ("selectWorld.recreate".equals(key)) {
                    recreate = widget;
                } else if ("gui.back".equals(key)) {
                    back = widget;
                }
            }
        }

        if (play == null || create == null || edit == null || delete == null || recreate == null || back == null) {
            return;
        }

        int panelLeft = rx + 20;
        int panelWidthAvailable = rw - 40;

        int columnGap = 4;
        int innerGap = 4;

        int columnWidth = (panelWidthAvailable - columnGap) / 2;
        int smallButtonWidth = (columnWidth - innerGap) / 2;
        int largeButtonWidth = smallButtonWidth * 2 + innerGap;

        int rightColumnX = panelLeft + columnWidth + columnGap;

        int topRowY = this.height - 52;
        int bottomRowY = topRowY + 22;

        play.setX(panelLeft);
        play.setY(topRowY);
        play.setWidth(largeButtonWidth);

        create.setX(rightColumnX);
        create.setY(topRowY);
        create.setWidth(largeButtonWidth);

        edit.setX(panelLeft);
        edit.setY(bottomRowY);
        edit.setWidth(smallButtonWidth);

        delete.setX(panelLeft + smallButtonWidth + innerGap);
        delete.setY(bottomRowY);
        delete.setWidth(smallButtonWidth);

        recreate.setX(rightColumnX);
        recreate.setY(bottomRowY);
        recreate.setWidth(smallButtonWidth);

        back.setX(rightColumnX + smallButtonWidth + innerGap);
        back.setY(bottomRowY);
        back.setWidth(smallButtonWidth);
    }

    private String getWidgetKey(AbstractWidget widget) {
        Component message = widget.getMessage();
        if (message instanceof MutableComponent mutable && mutable.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return message.getString();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= 0 && mouseX < this.panelWidth) {
            int contentHeight = getSidebarContentHeight();
            int visibleHeight = this.height - 40;
            int maxScroll = Math.max(0, contentHeight - visibleHeight);
            this.sidebarTargetScroll = Mth.clamp(this.sidebarTargetScroll - (float) scrollY * 16.0f, 0.0f, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float tick) {
        int originalWidth = this.width;
        int rw = this.width - this.panelWidth;
        int desiredCenter = this.panelWidth + rw / 2;

        this.width = desiredCenter * 2;
        super.render(gui, mouseX, mouseY, tick);
        this.width = originalWidth;

        int alpha = Config.PANEL_ALPHA.get();
        int bgColor = Config.PANEL_BACKGROUND_STYLE.get() == Config.BackgroundStyle.GRAY ? 0x1A1A1A : 0x000000;

        gui.fill(0, 0, this.panelWidth, this.height, (alpha << 24) | bgColor);
        gui.fill(this.panelWidth - 1, 0, this.panelWidth, this.height, 0x66555555);

        this.sidebarScroll = Mth.lerp(0.2f, this.sidebarScroll, this.sidebarTargetScroll);

        renderSidebarInfo(gui);
    }

    private int getSidebarContentHeight() {
        if (this.cachedList == null || !(this.cachedList.getSelected() instanceof WorldSelectionList.WorldListEntry)) return 0;

        int y = 20;
        int pW = this.panelWidth - 20;

        if (Config.SHOW_LARGE_ICON.get()) {
            int iconHeight = pW * 9 / 16;
            y += iconHeight + 12;
        }

        if (Config.SHOW_WORLD_NAME.get()) y += this.font.lineHeight + 4;
        if (Config.SHOW_FOLDER_NAME.get()) y += this.font.lineHeight + 8;
        else y += 6;

        if (Config.SHOW_GAME_MODE.get()) y += 12;
        if (Config.SHOW_DIFFICULTY.get()) y += 12;
        if (Config.SHOW_TIME_PLAYED.get()) y += 12;
        if (Config.SHOW_VERSION.get()) y += 12;
        if (Config.SHOW_CHEATS.get()) y += 12;

        y += 6;

        if (!Config.CUSTOM_LINE_1_LABEL.get().isEmpty() || !Config.CUSTOM_LINE_1_FORMAT.get().isEmpty()) y += 12;
        if (!Config.CUSTOM_LINE_2_LABEL.get().isEmpty() || !Config.CUSTOM_LINE_2_FORMAT.get().isEmpty()) y += 12;
        if (!Config.CUSTOM_LINE_3_LABEL.get().isEmpty() || !Config.CUSTOM_LINE_3_FORMAT.get().isEmpty()) y += 12;
        if (!Config.CUSTOM_LINE_4_LABEL.get().isEmpty() || !Config.CUSTOM_LINE_4_FORMAT.get().isEmpty()) y += 12;
        if (!Config.CUSTOM_LINE_5_LABEL.get().isEmpty() || !Config.CUSTOM_LINE_5_FORMAT.get().isEmpty()) y += 12;
        if (!Config.CUSTOM_LINE_6_LABEL.get().isEmpty() || !Config.CUSTOM_LINE_6_FORMAT.get().isEmpty()) y += 12;

        if (Config.SHOW_LAST_PLAYED.get()) y += this.font.lineHeight + 5;

        return y + 10;
    }

    private void renderSidebarInfo(GuiGraphics gui) {
        if (this.cachedList == null || !(this.cachedList.getSelected() instanceof WorldSelectionList.WorldListEntry entry)) return;

        int mouseX = (int) (Minecraft.getInstance().mouseHandler.xpos() * this.width / Minecraft.getInstance().getWindow().getScreenWidth());
        int mouseY = (int) (Minecraft.getInstance().mouseHandler.ypos() * this.height / Minecraft.getInstance().getWindow().getScreenHeight());

        LevelSummary s = ((WorldListEntryAccessor) (Object) entry).getSummary();
        int cx = this.panelWidth / 2;
        int y = 20 - (int) this.sidebarScroll;
        int pW = this.panelWidth - 20;

        gui.enableScissor(0, 0, this.panelWidth, this.height);

        if (Config.SHOW_LARGE_ICON.get()) {
            int iconHeight = pW * 9 / 16;
            gui.blit(loadLargeIcon(s), cx - pW / 2, y, 0, 0, pW, iconHeight, pW, iconHeight);
            y += iconHeight + 12;
        }

        if (Config.SHOW_WORLD_NAME.get()) {
            y = drawScaled(gui, s.getLevelName(), cx, y, 0xFFFFFF, pW, true, false);
            y += 2;
        }

        if (Config.SHOW_FOLDER_NAME.get()) {
            y = drawScaled(gui, "(" + s.getLevelId() + ")", cx, y, 0x777777, pW, false, true);
            y += 8;
        } else {
            y += 6;
        }

        if (Config.SHOW_GAME_MODE.get()) {
            String modeText = s.isHardcore()
                    ? Component.translatable("reimagined_world_selection.value.hardcore").getString()
                    : s.getGameMode().getName().substring(0, 1).toUpperCase() + s.getGameMode().getName().substring(1);
            int modeColor = s.isHardcore() ? 0xFFFF0000 : 0xAAAAAA;
            y = renderAnimatedLine(gui, Component.translatable("reimagined_world_selection.label.mode").getString(), modeText, cx, y, modeColor, pW, mouseX, mouseY);
        }

        if (Config.SHOW_DIFFICULTY.get()) {
            Path worldDir = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(s.getLevelId());
            WorldInfoHelper.DifficultyInfo difficultyInfo = WorldInfoHelper.readDifficulty(worldDir);

            if (difficultyInfo != null) {
                DifficultyRenderData data = getDifficultyRenderData(difficultyInfo);
                y = renderAnimatedLine(gui, Component.translatable("reimagined_world_selection.label.difficulty").getString(), data.text(), cx, y, data.color(), pW, mouseX, mouseY);
            }
        }

        if (Config.SHOW_TIME_PLAYED.get()) {
            Path worldDir = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(s.getLevelId());
            long playTime = WorldInfoHelper.readPlayerPlayTime(worldDir);
            y = renderAnimatedLine(gui, Component.translatable("reimagined_world_selection.label.time_played").getString(), formatPlayTime(playTime), cx, y, pW, mouseX, mouseY);
        }

        if (Config.SHOW_VERSION.get()) {
            y = renderAnimatedLine(gui, Component.translatable("reimagined_world_selection.label.version").getString(), s.levelVersion().minecraftVersionName(), cx, y, pW, mouseX, mouseY);
        }

        if (Config.SHOW_CHEATS.get()) {
            Integer redColorObj = ChatFormatting.RED.getColor();
            int cheatsColor = redColorObj != null && s.hasCommands() ? redColorObj : 0xAAAAAA;
            y = renderAnimatedLine(gui, Component.translatable("reimagined_world_selection.label.commands").getString(), s.hasCommands() ? "ON" : "OFF", cx, y, cheatsColor, pW, mouseX, mouseY);
        }

        y += 6;

        Map<String, String> worldVars = WorldSelectionAPI.getWorldVariables(s.getLevelId());
        Map<String, String> playerVars = WorldSelectionAPI.getPlayerVariables(s.getLevelId());

        y = renderCustomAnimatedLine(gui, Config.CUSTOM_LINE_1_LABEL.get(), Config.CUSTOM_LINE_1_FORMAT.get(), Config.CUSTOM_LINE_1_MODE.get(), Config.CUSTOM_LINE_1_SOURCE.get(), worldVars, playerVars, 1, s, cx, y, pW, mouseX, mouseY);
        y = renderCustomAnimatedLine(gui, Config.CUSTOM_LINE_2_LABEL.get(), Config.CUSTOM_LINE_2_FORMAT.get(), Config.CUSTOM_LINE_2_MODE.get(), Config.CUSTOM_LINE_2_SOURCE.get(), worldVars, playerVars, 2, s, cx, y, pW, mouseX, mouseY);
        y = renderCustomAnimatedLine(gui, Config.CUSTOM_LINE_3_LABEL.get(), Config.CUSTOM_LINE_3_FORMAT.get(), Config.CUSTOM_LINE_3_MODE.get(), Config.CUSTOM_LINE_3_SOURCE.get(), worldVars, playerVars, 3, s, cx, y, pW, mouseX, mouseY);
        y = renderCustomAnimatedLine(gui, Config.CUSTOM_LINE_4_LABEL.get(), Config.CUSTOM_LINE_4_FORMAT.get(), Config.CUSTOM_LINE_4_MODE.get(), Config.CUSTOM_LINE_4_SOURCE.get(), worldVars, playerVars, 4, s, cx, y, pW, mouseX, mouseY);
        y = renderCustomAnimatedLine(gui, Config.CUSTOM_LINE_5_LABEL.get(), Config.CUSTOM_LINE_5_FORMAT.get(), Config.CUSTOM_LINE_5_MODE.get(), Config.CUSTOM_LINE_5_SOURCE.get(), worldVars, playerVars, 5, s, cx, y, pW, mouseX, mouseY);
        y = renderCustomAnimatedLine(gui, Config.CUSTOM_LINE_6_LABEL.get(), Config.CUSTOM_LINE_6_FORMAT.get(), Config.CUSTOM_LINE_6_MODE.get(), Config.CUSTOM_LINE_6_SOURCE.get(), worldVars, playerVars, 6, s, cx, y, pW, mouseX, mouseY);

        if (Config.SHOW_LAST_PLAYED.get()) {
            y += 3;
            String dateStr = "(" + dateFormat.format(new Date(s.getLastPlayed())) + ")";
            drawScaled(gui, dateStr, cx, y, 0x777777, pW, false, true);
        }

        gui.disableScissor();
    }

    private int renderCustomAnimatedLine(GuiGraphics gui, String label, String format, Config.VarMode mode, Config.VariableSource source, Map<String, String> worldVars, Map<String, String> playerVars, int index, LevelSummary s, int cx, int y, int maxWidth, int mouseX, int mouseY) {
        if (label.isEmpty() && format.isEmpty()) return y;

        String parsedValue = getCustomLineValue(format, mode, source, worldVars, playerVars, index, s);

        if (!label.isEmpty() && !label.endsWith(" ")) {
            label += ": ";
        }

        return renderAnimatedLine(gui, label, parsedValue, cx, y, maxWidth, mouseX, mouseY);
    }

    private String getCustomLineValue(String format, Config.VarMode mode, Config.VariableSource source, Map<String, String> worldVars, Map<String, String> playerVars, int index, LevelSummary s) {
        Map<String, String> selectedVars = source == Config.VariableSource.PLAYER ? playerVars : worldVars;
        String rawVar = selectedVars.get("var" + index);
        String interpretedVar = interpretVar(rawVar, mode);
        return parsePlaceholders(format, s).replace("%var%", interpretedVar);
    }

    private String interpretVar(@Nullable String rawVar, Config.VarMode mode) {
        if (mode == Config.VarMode.VALUE) {
            return rawVar == null ? "" : rawVar;
        }

        double numericValue = 0;
        try {
            if (rawVar != null && !rawVar.isEmpty()) {
                numericValue = Double.parseDouble(rawVar);
            }
        } catch (NumberFormatException e) {
            numericValue = 0;
        }

        if (mode == Config.VarMode.BOOLEAN) {
            return numericValue >= 1 ? "True" : "False";
        } else if (mode == Config.VarMode.ON_OFF) {
            return numericValue >= 1 ? "ON" : "OFF";
        }

        return rawVar == null ? "" : rawVar;
    }

    private String parsePlaceholders(String text, LevelSummary s) {
        if (text.isEmpty()) return "";

        String modeText = s.isHardcore() ? Component.translatable("reimagined_world_selection.value.hardcore").getString() : s.getGameMode().getName();

        return text
                .replace("%name%", s.getLevelName())
                .replace("%id%", s.getLevelId())
                .replace("%mode%", modeText)
                .replace("%version%", s.levelVersion().minecraftVersionName())
                .replace("%cheats%", s.hasCommands() ? "ON" : "OFF")
                .replace("%hardcore%", s.isHardcore() ? "Yes" : "No");
    }

    private String formatPlayTime(long ticks) {
        double minutes = ticks / 20.0 / 60.0;
        double value;
        String unit;

        switch (Config.TIME_PLAYED_FORMAT.get()) {
            case MINUTES -> {
                value = minutes;
                unit = Component.translatable("reimagined_world_selection.time.minutes").getString();
            }
            case HOURS -> {
                value = minutes / 60.0;
                unit = Component.translatable("reimagined_world_selection.time.hours").getString();
            }
            case DAYS -> {
                value = minutes / 60.0 / 24.0;
                unit = Component.translatable("reimagined_world_selection.time.days").getString();
            }
            case WEEKS -> {
                value = minutes / 60.0 / 24.0 / 7.0;
                unit = Component.translatable("reimagined_world_selection.time.weeks").getString();
            }
            case MONTHS -> {
                value = minutes / 60.0 / 24.0 / 30.0;
                unit = Component.translatable("reimagined_world_selection.time.months").getString();
            }
            case YEARS -> {
                value = minutes / 60.0 / 24.0 / 365.0;
                unit = Component.translatable("reimagined_world_selection.time.years").getString();
            }
            default -> throw new IllegalStateException("Unexpected value: " + Config.TIME_PLAYED_FORMAT.get());
        }

        String formatted = value >= 100 ? String.format(Locale.ROOT, "%.0f", value) : String.format(Locale.ROOT, "%.1f", value);
        if (formatted.endsWith(".0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
        }
        return formatted + unit;
    }

    private int drawScaled(GuiGraphics g, String text, int x, int y, int color, int maxWidth, boolean bold, boolean italic) {
        Style st = Style.EMPTY.withBold(bold).withItalic(italic);
        FormattedCharSequence fcs = Component.literal(text).withStyle(st).getVisualOrderText();
        float tw = this.font.width(fcs);
        float scale = tw > maxWidth ? (float) maxWidth / tw : 1.0f;

        g.pose().pushPose();
        g.pose().translate(x, y + (this.font.lineHeight * (1.0f - scale) / 2.0f), 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(this.font, fcs, (int) (-tw / 2), 0, color, true);
        g.pose().popPose();

        return y + (int) (this.font.lineHeight * scale) + 2;
    }

    private int renderAnimatedLine(GuiGraphics g, String label, String value, int cx, int y, int maxWidth, int mouseX, int mouseY) {
        return renderAnimatedLine(g, label, value, cx, y, 0xAAAAAA, maxWidth, mouseX, mouseY);
    }

    private int renderAnimatedLine(GuiGraphics g, String label, String value, int cx, int y, int valueColor, int maxWidth, int mouseX, int mouseY) {
        String full = label + value;
        float tw = this.font.width(full);
        float baseScale = tw > maxWidth ? (float) maxWidth / tw : 1.0f;

        boolean hovered = mouseX >= 0 && mouseX <= this.panelWidth && mouseY >= y - 2 && mouseY <= y + this.font.lineHeight + 2;

        String animKey = label + "|" + value + "|" + y;
        float currentAnim = this.sidebarLineAnimations.getOrDefault(animKey, 0.0f);
        float targetAnim = Config.ENABLE_TEXT_HOVER_ANIMATION.get() && hovered ? 1.0f : 0.0f;
        currentAnim = Mth.lerp(0.18f, currentAnim, targetAnim);
        if (Math.abs(currentAnim - targetAnim) < 0.01f) {
            currentAnim = targetAnim;
        }
        this.sidebarLineAnimations.put(animKey, currentAnim);

        float hoverScale = Mth.lerp(currentAnim, 1.0f, 1.08f);
        float finalScale = baseScale * hoverScale;

        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(finalScale, finalScale, 1.0f);

        int startX = (int) (-tw / 2);
        g.drawString(this.font, label, startX, 0, 0x555555, false);
        g.drawString(this.font, value, startX + this.font.width(label), 0, valueColor, false);

        g.pose().popPose();
        return y + (int) (this.font.lineHeight * finalScale) + 2;
    }

    @Nonnull
    public ResourceLocation loadIcon(LevelSummary s) {
        return loadIconInternal(s, false);
    }

    @Nonnull
    public ResourceLocation loadLargeIcon(LevelSummary s) {
        return loadIconInternal(s, true);
    }

    @Nonnull
    private ResourceLocation loadIconInternal(LevelSummary s, boolean useLargeIcon) {
        String id = s.getLevelId();
        String cacheKey = (useLargeIcon ? "large_" : "") + id;

        if (iconCache.containsKey(cacheKey)) {
            return iconCache.get(cacheKey);
        }

        Path worldDir = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(id);
        Path targetPath;

        if (useLargeIcon) {
            Path customLargeIcon = worldDir.resolve("custom_large_icon.png");
            Path largeIcon = worldDir.resolve("large_icon.png");
            Path normalIcon = worldDir.resolve("icon.png");

            if (Files.exists(customLargeIcon)) {
                targetPath = customLargeIcon;
            } else if (Files.exists(largeIcon)) {
                targetPath = largeIcon;
            } else {
                targetPath = normalIcon;
            }
        } else {
            targetPath = worldDir.resolve("icon.png");
        }

        if (Files.exists(targetPath)) {
            try (InputStream is = Files.newInputStream(targetPath)) {
                DynamicTexture dt = new DynamicTexture(NativeImage.read(is));
                String prefix = useLargeIcon ? "large_icon_" : "icon_";
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(
                        "reimagined",
                        prefix + id.toLowerCase().replaceAll("[^a-z0-9_]", "_")
                );
                Minecraft.getInstance().getTextureManager().register(rl, dt);
                iconCache.put(cacheKey, rl);
                return rl;
            } catch (Exception e) {
                LOGGER.warn("Failed to load icon for world: {}", id, e);
            }
        }

        return DEFAULT_ICON;
    }

    private DifficultyRenderData getDifficultyRenderData(WorldInfoHelper.DifficultyInfo difficultyInfo) {
        String difficultyText;
        int difficultyColor;

        switch (difficultyInfo.difficultyId()) {
            case 0 -> {
                difficultyText = Component.translatable("reimagined_world_selection.value.peaceful").getString();
                difficultyColor = 0xFFFFFF;
            }
            case 1 -> {
                difficultyText = Component.translatable("reimagined_world_selection.value.easy").getString();
                difficultyColor = 0x55FF55;
            }
            case 2 -> {
                difficultyText = Component.translatable("reimagined_world_selection.value.normal").getString();
                difficultyColor = 0xFFFF55;
            }
            case 3 -> {
                difficultyText = Component.translatable("reimagined_world_selection.value.hard").getString();
                difficultyColor = 0xFF5555;
            }
            default -> {
                difficultyText = Component.translatable("reimagined_world_selection.value.unknown").getString();
                difficultyColor = 0xAAAAAA;
            }
        }

        if (difficultyInfo.locked()) {
            difficultyText += " " + Component.translatable("reimagined_world_selection.value.locked").getString();
            difficultyColor = 0x555555;
        }

        return new DifficultyRenderData(difficultyText, difficultyColor);
    }

    @Override
    public void onClose() {
        iconCache.values().forEach(loc -> {
            if (!loc.equals(DEFAULT_ICON)) Minecraft.getInstance().getTextureManager().release(loc);
        });
        iconCache.clear();
        sidebarLineAnimations.clear();
        WorldSelectionAPI.clearCache();
        super.onClose();
    }

    private record DifficultyRenderData(String text, int color) {}
}