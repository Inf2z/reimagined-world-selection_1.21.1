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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.InputStream;
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
    private final Map<String, Float> sidebarLineAnimations = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private static final ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_server.png");

    private float sidebarScroll;
    private float sidebarTargetScroll;

    private static boolean panelVisible = true;
    private float panelAnimationProgress = panelVisible ? 1.0f : 0.0f;

    private final Map<AbstractWidget, Integer> originalButtonWidths = new HashMap<>();

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

    private void togglePanel() {
        panelVisible = !panelVisible;
    }

    private boolean isPanelDynamic() {
        return Config.PANEL_BEHAVIOUR.get() == Config.PanelBehaviour.DYNAMIC;
    }

    private boolean isPanelMixed() {
        return Config.PANEL_BEHAVIOUR.get() == Config.PanelBehaviour.MIXED;
    }

    private boolean hasWorldSelected() {
        return this.cachedList != null && this.cachedList.getSelected() instanceof WorldSelectionList.WorldListEntry;
    }

    private void applyReimaginedLayout() {
        this.panelWidth = (int) (this.width * Config.PANEL_WIDTH_RATIO.get());

        if (isPanelDynamic()) {
        } else if (isPanelMixed()) {
            panelVisible = hasWorldSelected();
            panelAnimationProgress = panelVisible ? 1.0f : 0.0f;
        } else {
            panelVisible = true;
            panelAnimationProgress = 1.0f;
        }

        saveOriginalButtonWidths();
        updateLayout();
    }

    private void saveOriginalButtonWidths() {
        int fullPanelWidth = this.panelWidth;
        int rw = this.width - fullPanelWidth;
        int panelWidthAvailable = rw - 40;

        int columnGap = 4;
        int innerGap = 4;

        int columnWidth = (panelWidthAvailable - columnGap) / 2;
        int smallButtonWidth = (columnWidth - innerGap) / 2;
        int largeButtonWidth = smallButtonWidth * 2 + innerGap;

        for (GuiEventListener listener : this.children()) {
            if (listener instanceof AbstractWidget widget && widget.getY() > this.height - 80) {
                String key = getWidgetKey(widget);

                if ("selectWorld.select".equals(key) || "selectWorld.create".equals(key)) {
                    originalButtonWidths.put(widget, largeButtonWidth);
                } else if ("selectWorld.edit".equals(key) || "selectWorld.delete".equals(key) ||
                        "selectWorld.recreate".equals(key) || "gui.back".equals(key)) {
                    originalButtonWidths.put(widget, smallButtonWidth);
                }
            }
        }
    }

    private void updateLayout() {
        int currentPanelWidth = (int) (this.panelWidth * panelAnimationProgress);
        int rw = this.width - currentPanelWidth;

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
                    list.setX(currentPanelWidth + 20);
                } else if (widget instanceof EditBox search) {
                    search.setY(22);
                    search.setX(currentPanelWidth + (rw - search.getWidth()) / 2);
                }
            }
        }

        layoutFooterButtons(currentPanelWidth, rw);
    }

    private void layoutFooterButtons(int panelOffset, int rw) {
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

        int largeButtonWidth = originalButtonWidths.getOrDefault(play, 150);
        int smallButtonWidth = originalButtonWidths.getOrDefault(edit, 73);

        int columnGap = 4;
        int innerGap = 4;

        int totalWidth = largeButtonWidth * 2 + columnGap;
        int panelLeft = panelOffset + (rw - totalWidth) / 2;
        int rightColumnX = panelLeft + largeButtonWidth + columnGap;

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
        int currentPanelWidth = (int) (this.panelWidth * panelAnimationProgress);
        if (mouseX >= 0 && mouseX < currentPanelWidth) {
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
        if (isPanelMixed()) {
            boolean shouldShow = hasWorldSelected();
            if (panelVisible != shouldShow) {
                panelVisible = shouldShow;
            }
        }

        if (isPanelDynamic() || isPanelMixed()) {
            float targetProgress = panelVisible ? 1.0f : 0.0f;
            float animationSpeed = 0.15f;

            if (Math.abs(panelAnimationProgress - targetProgress) > 0.001f) {
                panelAnimationProgress = Mth.lerp(animationSpeed, panelAnimationProgress, targetProgress);
                updateLayout();
            } else {
                panelAnimationProgress = targetProgress;
            }
        }

        int currentPanelWidth = (int) (this.panelWidth * panelAnimationProgress);
        int originalWidth = this.width;
        int rw = this.width - currentPanelWidth;
        int desiredCenter = currentPanelWidth + rw / 2;

        this.width = desiredCenter * 2;
        super.render(gui, mouseX, mouseY, tick);
        this.width = originalWidth;

        if (panelAnimationProgress > 0.001f) {
            int alpha = Config.PANEL_ALPHA.get();
            int bgColor = Config.PANEL_BACKGROUND_STYLE.get() == Config.BackgroundStyle.GRAY ? 0x1A1A1A : 0x000000;

            gui.fill(0, 0, currentPanelWidth, this.height, (alpha << 24) | bgColor);
            gui.fill(currentPanelWidth - 1, 0, currentPanelWidth, this.height, 0x66555555);

            this.sidebarScroll = Mth.lerp(0.2f, this.sidebarScroll, this.sidebarTargetScroll);

            renderSidebarInfo(gui);
        }

        if (isPanelDynamic()) {
            renderToggleButton(gui, mouseX, mouseY);
        }
    }

    private void renderToggleButton(GuiGraphics gui, int mouseX, int mouseY) {
        int currentPanelWidth = (int) (this.panelWidth * panelAnimationProgress);
        int buttonWidth = 12;
        int buttonHeight = 17;
        int toggleButtonX = currentPanelWidth;
        int toggleButtonY = (this.height - buttonHeight) / 2;

        boolean hovered = mouseX >= toggleButtonX && mouseX <= toggleButtonX + buttonWidth &&
                mouseY >= toggleButtonY && mouseY <= toggleButtonY + buttonHeight;

        int bgColor = hovered ? 0x80404040 : 0x60303030;
        gui.fill(toggleButtonX, toggleButtonY, toggleButtonX + buttonWidth, toggleButtonY + buttonHeight, bgColor);

        gui.fill(toggleButtonX + buttonWidth - 1, toggleButtonY, toggleButtonX + buttonWidth, toggleButtonY + buttonHeight, 0x66555555);

        String arrow = panelVisible ? "<" : ">";
        int textWidth = this.font.width(arrow);
        int textX = toggleButtonX + (buttonWidth - textWidth) / 2;
        int textY = toggleButtonY + (buttonHeight - this.font.lineHeight) / 2;

        int textColor = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;

        gui.drawString(this.font, arrow, textX, textY, textColor, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isPanelDynamic()) {
            int currentPanelWidth = (int) (this.panelWidth * panelAnimationProgress);
            int buttonWidth = 12;
            int buttonHeight = 17;
            int toggleButtonX = currentPanelWidth;
            int toggleButtonY = (this.height - buttonHeight) / 2;

            if (mouseX >= toggleButtonX && mouseX <= toggleButtonX + buttonWidth &&
                    mouseY >= toggleButtonY && mouseY <= toggleButtonY + buttonHeight && button == 0) {
                togglePanel();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int getSidebarContentHeight() {
        if (this.cachedList == null || !(this.cachedList.getSelected() instanceof WorldSelectionList.WorldListEntry)) return 0;

        int y = 20;
        int pW = this.panelWidth - 20;

        List<String> order = (List<String>) Config.PANEL_ORDER.get();
        if (order.isEmpty()) {
            order = getDefaultOrder();
        }

        for (String id : order) {
            y = addHeightForElement(id, y, pW);
        }

        List<Config.CustomLineConfig> customLines = Config.getCustomLines();
        for (String orderId : order) {
            if (orderId.startsWith("custom_")) {
                y += 12;
            }
        }

        int customsInOrder = (int) order.stream().filter(s -> s.startsWith("custom_")).count();
        int extraCustoms = customLines.size() - customsInOrder;
        if (extraCustoms > 0) {
            y += extraCustoms * 12;
        }

        return y + 10;
    }

    private int addHeightForElement(String id, int y, int pW) {
        if (id.startsWith("custom_")) {
            return y + 12;
        }
        return switch (id) {
            case "large_icon" -> Config.SHOW_LARGE_ICON.get() ? y + (pW * 9 / 16) + 12 : y;
            case "world_name" -> Config.SHOW_WORLD_NAME.get() ? y + this.font.lineHeight + 4 : y;
            case "folder_name" -> Config.SHOW_FOLDER_NAME.get() ? y + this.font.lineHeight + 8 : y;
            case "game_mode" -> Config.SHOW_GAME_MODE.get() ? y + 12 : y;
            case "difficulty" -> Config.SHOW_DIFFICULTY.get() ? y + 12 : y;
            case "time_played" -> Config.SHOW_TIME_PLAYED.get() ? y + 12 : y;
            case "version" -> Config.SHOW_VERSION.get() ? y + 12 : y;
            case "cheats" -> Config.SHOW_CHEATS.get() ? y + 12 : y;
            case "last_played" -> Config.SHOW_LAST_PLAYED.get() ? y + this.font.lineHeight + 5 : y;
            default -> y;
        };
    }

    private List<String> getDefaultOrder() {
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

    private void renderSidebarInfo(GuiGraphics gui) {
        if (this.cachedList == null || !(this.cachedList.getSelected() instanceof WorldSelectionList.WorldListEntry entry)) return;
        if (panelAnimationProgress < 0.001f) return;

        int mouseX = (int) (Minecraft.getInstance().mouseHandler.xpos() * this.width / Minecraft.getInstance().getWindow().getScreenWidth());
        int mouseY = (int) (Minecraft.getInstance().mouseHandler.ypos() * this.height / Minecraft.getInstance().getWindow().getScreenHeight());

        LevelSummary s = ((WorldListEntryAccessor) (Object) entry).getSummary();

        int currentPanelWidth = (int) (this.panelWidth * panelAnimationProgress);
        int offset = this.panelWidth - currentPanelWidth;
        int cx = this.panelWidth / 2 - offset;
        int y = 20 - (int) this.sidebarScroll;
        int pW = this.panelWidth - 20;

        gui.enableScissor(0, 0, currentPanelWidth, this.height);

        List<String> order = (List<String>) Config.PANEL_ORDER.get();
        if (order.isEmpty()) {
            order = getDefaultOrder();
        }

        List<Config.CustomLineConfig> customLines = Config.getCustomLines();
        Map<String, String> worldVars = WorldSelectionAPI.getWorldVariables(s.getLevelId());
        Map<String, String> playerVars = WorldSelectionAPI.getPlayerVariables(s.getLevelId());

        Set<Integer> renderedCustomIndices = new HashSet<>();

        for (String id : order) {
            if (id.startsWith("custom_")) {
                try {
                    int customIndex = Integer.parseInt(id.substring(7));
                    if (customIndex >= 0 && customIndex < customLines.size()) {
                        Config.CustomLineConfig line = customLines.get(customIndex);
                        y = renderCustomAnimatedLine(gui, line, worldVars, playerVars, s, cx, y, pW, mouseX, mouseY);
                        renderedCustomIndices.add(customIndex);
                    }
                } catch (NumberFormatException ignored) {}
            } else {
                y = renderElement(gui, id, s, cx, y, pW, mouseX, mouseY);
            }
        }

        for (int i = 0; i < customLines.size(); i++) {
            if (!renderedCustomIndices.contains(i)) {
                Config.CustomLineConfig line = customLines.get(i);
                y = renderCustomAnimatedLine(gui, line, worldVars, playerVars, s, cx, y, pW, mouseX, mouseY);
            }
        }

        gui.disableScissor();
    }

    private int renderElement(GuiGraphics gui, String id, LevelSummary s, int cx, int y, int pW, int mouseX, int mouseY) {
        return switch (id) {
            case "large_icon" -> {
                if (Config.SHOW_LARGE_ICON.get()) {
                    int iconHeight = pW * 9 / 16;
                    gui.blit(loadLargeIcon(s), cx - pW / 2, y, 0, 0, pW, iconHeight, pW, iconHeight);
                    yield y + iconHeight + 12;
                }
                yield y;
            }
            case "world_name" -> {
                if (Config.SHOW_WORLD_NAME.get()) {
                    int newY = drawScaled(gui, s.getLevelName(), cx, y, 0xFFFFFF, pW, true, false);
                    yield newY + 2;
                }
                yield y;
            }
            case "folder_name" -> {
                if (Config.SHOW_FOLDER_NAME.get()) {
                    int newY = drawScaled(gui, "(" + s.getLevelId() + ")", cx, y, 0x777777, pW, false, true);
                    yield newY + 8;
                }
                yield y + 6;
            }
            case "game_mode" -> {
                if (Config.SHOW_GAME_MODE.get()) {
                    String modeText;
                    int modeColor;

                    if (s.isHardcore()) {
                        modeText = Component.translatable("reimagined_world_selection.value.hardcore").getString();
                        modeColor = 0xFFFF0000;
                    } else {
                        String modeKey = switch (s.getGameMode()) {
                            case SURVIVAL -> "reimagined_world_selection.value.survival";
                            case CREATIVE -> "reimagined_world_selection.value.creative";
                            case ADVENTURE -> "reimagined_world_selection.value.adventure";
                            case SPECTATOR -> "reimagined_world_selection.value.spectator";
                        };
                        modeText = Component.translatable(modeKey).getString();
                        modeColor = 0xAAAAAA;
                    }

                    yield renderAnimatedLine(gui, Component.translatable("reimagined_world_selection.label.mode").getString(), modeText, cx, y, modeColor, pW, mouseX, mouseY);
                }
                yield y;
            }
            case "difficulty" -> {
                if (Config.SHOW_DIFFICULTY.get()) {
                    Path worldDir = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(s.getLevelId());
                    WorldInfoHelper.DifficultyInfo difficultyInfo = WorldInfoHelper.readDifficulty(worldDir);
                    if (difficultyInfo != null) {
                        DifficultyRenderData data = getDifficultyRenderData(difficultyInfo);
                        yield renderAnimatedLine(gui, Component.translatable("reimagined_world_selection.label.difficulty").getString(), data.text(), cx, y, data.color(), pW, mouseX, mouseY);
                    }
                }
                yield y;
            }
            case "time_played" -> {
                if (Config.SHOW_TIME_PLAYED.get()) {
                    Path worldDir = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(s.getLevelId());
                    long playTime = WorldInfoHelper.readPlayerPlayTime(worldDir);
                    yield renderAnimatedLine(gui, Component.translatable("reimagined_world_selection.label.time_played").getString(), formatPlayTime(playTime), cx, y, pW, mouseX, mouseY);
                }
                yield y;
            }
            case "version" -> {
                if (Config.SHOW_VERSION.get()) {
                    yield renderAnimatedLine(gui, Component.translatable("reimagined_world_selection.label.version").getString(), s.levelVersion().minecraftVersionName(), cx, y, pW, mouseX, mouseY);
                }
                yield y;
            }
            case "cheats" -> {
                if (Config.SHOW_CHEATS.get()) {
                    Integer redColorObj = ChatFormatting.RED.getColor();
                    int cheatsColor = redColorObj != null && s.hasCommands() ? redColorObj : 0xAAAAAA;
                    yield renderAnimatedLine(gui,
                            Component.translatable("reimagined_world_selection.label.commands").getString(),
                            s.hasCommands()
                                    ? Component.translatable("reimagined_world_selection.value.on").getString()
                                    : Component.translatable("reimagined_world_selection.value.off").getString(),
                            cx, y, cheatsColor, pW, mouseX, mouseY);
                }
                yield y;
            }
            case "last_played" -> {
                if (Config.SHOW_LAST_PLAYED.get()) {
                    String dateStr = "(" + dateFormat.format(new Date(s.getLastPlayed())) + ")";
                    int newY = drawScaled(gui, dateStr, cx, y + 3, 0x777777, pW, false, true);
                    yield newY + 5;
                }
                yield y;
            }
            default -> y;
        };
    }

    private int renderCustomAnimatedLine(GuiGraphics gui, Config.CustomLineConfig line,
                                         Map<String, String> worldVars, Map<String, String> playerVars,
                                         LevelSummary s, int cx, int y, int maxWidth, int mouseX, int mouseY) {
        if (line.isEmpty()) return y;

        Map<String, String> selectedVars = line.source() == Config.VariableSource.PLAYER ? playerVars : worldVars;
        String rawVar = selectedVars.get("var" + line.varIndex());
        String interpretedVar = interpretVar(rawVar, line.mode());
        String parsedValue = parsePlaceholders(line.format(), s).replace("%var%", interpretedVar);

        String label = line.label();
        if (!label.isEmpty() && !label.endsWith(" ")) {
            label += ": ";
        }

        return renderAnimatedLine(gui, label, parsedValue, cx, y, maxWidth, mouseX, mouseY);
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
            return numericValue >= 1 ? Component.translatable("reimagined_world_selection.value.true").getString() : Component.translatable("reimagined_world_selection.value.false").getString();
        } else if (mode == Config.VarMode.ON_OFF) {
            return numericValue >= 1 ? Component.translatable("reimagined_world_selection.value.on").getString() : Component.translatable("reimagined_world_selection.value.off").getString();
        }

        return rawVar == null ? "" : rawVar;
    }

    private String parsePlaceholders(String text, LevelSummary s) {
        if (text.isEmpty()) return "";

        String modeText = getString(s);

        return text
                .replace("%name%", s.getLevelName())
                .replace("%id%", s.getLevelId())
                .replace("%mode%", modeText)
                .replace("%version%", s.levelVersion().minecraftVersionName())
                .replace("%cheats%", s.hasCommands()
                        ? Component.translatable("reimagined_world_selection.value.on").getString()
                        : Component.translatable("reimagined_world_selection.value.off").getString())
                .replace("%hardcore%", s.isHardcore()
                        ? Component.translatable("reimagined_world_selection.value.yes").getString()
                        : Component.translatable("reimagined_world_selection.value.no").getString());
    }

    private static @NotNull String getString(LevelSummary s) {
        String modeText;
        if (s.isHardcore()) {
            modeText = Component.translatable("reimagined_world_selection.value.hardcore").getString();
        } else {
            String modeKey = switch (s.getGameMode()) {
                case SURVIVAL -> "reimagined_world_selection.value.survival";
                case CREATIVE -> "reimagined_world_selection.value.creative";
                case ADVENTURE -> "reimagined_world_selection.value.adventure";
                case SPECTATOR -> "reimagined_world_selection.value.spectator";
            };
            modeText = Component.translatable(modeKey).getString();
        }
        return modeText;
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

        int currentPanelWidth = (int) (this.panelWidth * panelAnimationProgress);
        boolean hovered = mouseX >= 0 && mouseX <= currentPanelWidth && mouseY >= y - 2 && mouseY <= y + this.font.lineHeight + 2;

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