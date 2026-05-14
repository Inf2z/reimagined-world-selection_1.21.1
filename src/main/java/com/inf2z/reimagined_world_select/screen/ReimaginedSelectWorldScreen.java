package com.inf2z.reimagined_world_select.screen;

import com.inf2z.reimagined_world_select.Config;
import com.inf2z.reimagined_world_select.api.RWSCustomInfoAPI;
import com.inf2z.reimagined_world_select.mixin.WorldListEntryAccessor;
import com.inf2z.reimagined_world_select.util.CustomIconManager;
import com.inf2z.reimagined_world_select.util.GuiCompat;
import com.inf2z.reimagined_world_select.util.MultiSelectableList;
import com.inf2z.reimagined_world_select.util.SelectionListCompat;
import com.inf2z.reimagined_world_select.util.RenderCompat;
import com.inf2z.reimagined_world_select.util.TextureCompat;
import com.inf2z.reimagined_world_select.util.WorldInfoHelper;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
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
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
public class ReimaginedSelectWorldScreen extends SelectWorldScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReimaginedSelectWorldScreen.class);
    private static final ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_server.png");
    private static final LoadedImage DEFAULT_LOADED_ICON = new LoadedImage(DEFAULT_ICON, 64, 64);

    private static Method setColorMethod;
    private static boolean setColorResolved = false;

    private int panelWidth;
    private WorldSelectionList cachedList;
    private final Map<String, LoadedImage> staticTextureCache = new HashMap<>();
    private final Map<String, Float> sidebarLineAnimations = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private float sidebarScroll;
    private float sidebarTargetScroll;
    private static boolean panelVisible = true;
    private float panelAnimationProgress = panelVisible ? 1.0f : 0.0f;

    private int lastKnownWorldCount = -1;
    private boolean preloadDone = false;

    public ReimaginedSelectWorldScreen(@Nullable Screen lastScreen) {
        super(lastScreen != null ? lastScreen : new net.minecraft.client.gui.screens.TitleScreen());
    }

    private boolean isPanelLeft() { return Config.PANEL_POSITION.get() == Config.PanelPosition.LEFT; }
    private int currentPanelPx() { return (int) (this.panelWidth * panelAnimationProgress); }
    private int panelLeft() { return isPanelLeft() ? 0 : this.width - currentPanelPx(); }
    private int panelRight() { return panelLeft() + currentPanelPx(); }
    private int listAreaLeft() { return isPanelLeft() ? currentPanelPx() : 0; }
    private int listAreaWidth() { return this.width - currentPanelPx(); }
    private float panelTextScale() { return (float) Config.PANEL_TEXT_SCALE.get().doubleValue(); }
    private int scaledLineHeight() { return Math.max(1, Math.round(this.font.lineHeight * panelTextScale())); }
    private int singleInfoLineHeight() { return Math.max(10, scaledLineHeight() + 2); }

    @Override
    protected void init() {
        super.init();
        for (GuiEventListener listener : this.children()) {
            if (listener instanceof WorldSelectionList list) { this.cachedList = list; break; }
        }
        applyReimaginedLayout();
        wrapDeleteButton();
        preloadDone = false;
        lastKnownWorldCount = -1;
    }

    private void tryPreloadWorldIcons() {
        if (this.cachedList == null) return;
        int currentCount = this.cachedList.children().size();
        if (currentCount == 0) return;
        if (preloadDone && currentCount == lastKnownWorldCount) return;
        lastKnownWorldCount = currentCount;
        preloadDone = true;

        for (var child : this.cachedList.children()) {
            if (child instanceof WorldSelectionList.WorldListEntry entry) {
                WorldListEntryAccessor acc = (WorldListEntryAccessor) (Object) entry;
                LevelSummary summary = acc.getSummary();
                if (summary != null) {
                    CustomIconManager.preloadWorld(summary.getLevelId());
                }
            }
        }
    }

    @Override @Nonnull
    public Component getTitle() { return Component.empty(); }

    private void togglePanel() { panelVisible = !panelVisible; }
    private boolean isPanelDynamic() { return Config.PANEL_BEHAVIOUR.get() == Config.PanelBehaviour.DYNAMIC; }
    private boolean isPanelMixed() { return Config.PANEL_BEHAVIOUR.get() == Config.PanelBehaviour.MIXED; }
    private boolean hasWorldSelected() { return this.cachedList != null && this.cachedList.getSelected() instanceof WorldSelectionList.WorldListEntry; }

    @Nullable
    private MultiSelectableList getMultiList() { return this.cachedList instanceof MultiSelectableList multi ? multi : null; }
    private boolean hasMultiSelection() { return collectSelectedWorldEntries().size() > 1; }

    @Nullable
    private LevelSummary getSelectedSummary() {
        if (this.cachedList == null || !(this.cachedList.getSelected() instanceof WorldSelectionList.WorldListEntry entry)) return null;
        return ((WorldListEntryAccessor) (Object) entry).getSummary();
    }

    private Set<WorldSelectionList.WorldListEntry> collectSelectedWorldEntries() {
        Set<WorldSelectionList.WorldListEntry> selected = new LinkedHashSet<>();
        if (this.cachedList == null) return selected;
        MultiSelectableList multi = getMultiList();
        if (multi != null) {
            for (var child : this.cachedList.children()) {
                if (child instanceof WorldSelectionList.WorldListEntry entry && multi.rws$isMultiSelected(entry)) selected.add(entry);
            }
        }
        if (this.cachedList.getSelected() instanceof WorldSelectionList.WorldListEntry current) selected.add(current);
        return selected;
    }

    private void applyReimaginedLayout() {
        this.panelWidth = (int) (this.width * Config.PANEL_WIDTH_RATIO.get());
        if (isPanelMixed()) { panelVisible = hasWorldSelected(); panelAnimationProgress = panelVisible ? 1.0f : 0.0f; }
        else if (!isPanelDynamic()) { panelVisible = true; panelAnimationProgress = 1.0f; }
        if (this.cachedList == null) {
            for (GuiEventListener listener : this.children()) {
                if (listener instanceof WorldSelectionList list) { this.cachedList = list; break; }
            }
        }
        updateLayout();
    }

    private void updateLayout() {
        int laLeft = listAreaLeft();
        int laWidth = listAreaWidth();
        for (GuiEventListener listener : this.children()) {
            if (listener instanceof AbstractWidget widget) {
                if (widget.getY() < 32 && !(widget instanceof EditBox)) {
                    widget.visible = false; widget.active = false; widget.setX(-10000); continue;
                }
                if (widget instanceof WorldSelectionList list) {
                    this.cachedList = list; list.setWidth(laWidth - 40); list.setX(laLeft + 20);
                } else if (widget instanceof EditBox search) {
                    search.setY(22); search.setX(laLeft + (laWidth - search.getWidth()) / 2);
                }
            }
        }
        layoutFooterButtons(laLeft, laWidth);
    }

    private void layoutFooterButtons(int areaLeft, int areaWidth) {
        AbstractWidget play = null, create = null, edit = null, delete = null, recreate = null, back = null;
        for (GuiEventListener listener : this.children()) {
            if (listener instanceof AbstractWidget widget && widget.getY() > this.height - 80) {
                switch (getWidgetKey(widget)) {
                    case "selectWorld.select" -> play = widget;
                    case "selectWorld.create" -> create = widget;
                    case "selectWorld.edit" -> edit = widget;
                    case "selectWorld.delete" -> delete = widget;
                    case "selectWorld.recreate" -> recreate = widget;
                    case "gui.back" -> back = widget;
                }
            }
        }
        if (play == null || create == null || edit == null || delete == null || recreate == null || back == null) return;
        int fixedAreaWidth = this.width - this.panelWidth;
        int columnGap = 4, innerGap = 4;
        int columnWidth = (fixedAreaWidth - 40 - columnGap) / 2;
        int smallButtonWidth = (columnWidth - innerGap) / 2;
        int largeButtonWidth = smallButtonWidth * 2 + innerGap;
        int totalWidth = largeButtonWidth * 2 + columnGap;
        int leftColumnX = areaLeft + (areaWidth - totalWidth) / 2;
        int rightColumnX = leftColumnX + largeButtonWidth + columnGap;
        int topRowY = this.height - 52, bottomRowY = topRowY + 22;
        play.setX(leftColumnX); play.setY(topRowY); play.setWidth(largeButtonWidth);
        create.setX(rightColumnX); create.setY(topRowY); create.setWidth(largeButtonWidth);
        edit.setX(leftColumnX); edit.setY(bottomRowY); edit.setWidth(smallButtonWidth);
        delete.setX(leftColumnX + smallButtonWidth + innerGap); delete.setY(bottomRowY); delete.setWidth(smallButtonWidth);
        recreate.setX(rightColumnX); recreate.setY(bottomRowY); recreate.setWidth(smallButtonWidth);
        back.setX(rightColumnX + smallButtonWidth + innerGap); back.setY(bottomRowY); back.setWidth(smallButtonWidth);
    }

    private void wrapDeleteButton() {
        AbstractWidget deleteWidget = null;
        for (GuiEventListener listener : new ArrayList<>(this.children())) {
            if (listener instanceof AbstractWidget widget && "selectWorld.delete".equals(getWidgetKey(widget))) { deleteWidget = widget; break; }
        }
        if (deleteWidget == null) return;
        final AbstractWidget original = deleteWidget;
        this.removeWidget(original);
        Button newDelete = Button.builder(original.getMessage(), btn -> handleMultiDelete())
                .bounds(original.getX(), original.getY(), original.getWidth(), original.getHeight()).build();
        newDelete.active = original.active;
        this.addRenderableWidget(newDelete);
    }

    private void deleteWorldFiles(LevelSummary summary) {
        try {
            releaseCachedTexturesForWorld(summary.getLevelId());
            Path worldPath = Objects.requireNonNull(this.minecraft).getLevelSource().getBaseDir().resolve(summary.getLevelId());
            if (!Files.exists(worldPath)) return;
            IOException lastException = null;
            for (int attempt = 0; attempt < 5; attempt++) {
                try { deleteDirectory(worldPath); return; }
                catch (IOException e) { lastException = e; try { Thread.sleep(100L); } catch (InterruptedException ignored) {} }
            }
            if (lastException != null) throw lastException;
        } catch (Exception e) { LOGGER.error("Failed to delete world: {}", summary.getLevelId(), e); }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) { for (Path entry : entries.toList()) deleteDirectory(entry); }
        }
        Files.deleteIfExists(path);
    }

    private void handleMultiDelete() {
        Minecraft mc = Objects.requireNonNull(this.minecraft);
        MultiSelectableList multi = getMultiList();
        Set<WorldSelectionList.WorldListEntry> toDelete = collectSelectedWorldEntries();
        if (toDelete.isEmpty()) return;
        List<String> worldNames = new ArrayList<>();
        List<LevelSummary> summaries = new ArrayList<>();
        for (WorldSelectionList.WorldListEntry delEntry : toDelete) {
            WorldListEntryAccessor acc = (WorldListEntryAccessor) (Object) delEntry;
            LevelSummary sum = acc.getSummary();
            if (sum == null) continue;
            worldNames.add(sum.getLevelName());
            summaries.add(sum);
        }
        if (summaries.isEmpty()) return;
        if (summaries.size() == 1) {
            LevelSummary summary = summaries.getFirst();
            mc.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) { deleteWorldFiles(summary); if (multi != null) multi.rws$clearMultiSelection(); }
                mc.setScreen(new ReimaginedSelectWorldScreen(null));
            }, Component.translatable("selectWorld.deleteQuestion"), Component.translatable("selectWorld.deleteWarning", summary.getLevelName())));
        } else {
            mc.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) { for (LevelSummary sum : summaries) deleteWorldFiles(sum); if (multi != null) multi.rws$clearMultiSelection(); }
                mc.setScreen(new ReimaginedSelectWorldScreen(null));
            }, Component.translatable("reimagined_world_select.delete_confirm_title", summaries.size()), Component.literal(String.join(", ", worldNames))));
        }
    }

    private void updateButtonStatesForMultiSelect() {
        boolean multiSelected = hasMultiSelection();
        boolean anyWorldSelected = this.cachedList != null && this.cachedList.getSelected() instanceof WorldSelectionList.WorldListEntry;
        for (GuiEventListener listener : this.children()) {
            if (listener instanceof AbstractWidget widget && widget.getY() > this.height - 80) {
                switch (getWidgetKey(widget)) {
                    case "selectWorld.select", "selectWorld.edit", "selectWorld.recreate" -> { if (multiSelected) widget.active = false; }
                    case "selectWorld.delete" -> widget.active = anyWorldSelected;
                }
            }
        }
    }

    private String getWidgetKey(AbstractWidget widget) {
        Component message = widget.getMessage();
        if (message instanceof MutableComponent mutable && mutable.getContents() instanceof TranslatableContents translatable) return translatable.getKey();
        return message.getString();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= panelLeft() && mouseX < panelRight()) {
            int contentHeight = getSidebarContentHeight();
            int visibleHeight = this.height - 40;
            int maxScroll = Math.max(0, contentHeight - visibleHeight);
            this.sidebarTargetScroll = Mth.clamp(this.sidebarTargetScroll - (float) scrollY * 16.0f, 0.0f, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handleCustomIconFolderButtonClick(mouseX, mouseY)) return true;
        if (isPanelDynamic()) {
            int btnWidth = 12, btnHeight = 17;
            int btnY = (this.height - btnHeight) / 2;
            int btnX = isPanelLeft() ? currentPanelPx() : panelLeft() - btnWidth;
            if (mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight) { togglePanel(); return true; }
        }
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        updateButtonStatesForMultiSelect();
        return result;
    }

    private boolean handleCustomIconFolderButtonClick(double mouseX, double mouseY) {
        Rect rect = getLargeIconButtonRect();
        LevelSummary summary = getSelectedSummary();
        if (rect == null || summary == null) return false;
        if (!rect.contains(mouseX, mouseY)) return false;
        openCustomIconFolder(summary);
        return true;
    }

    private void openCustomIconFolder(LevelSummary summary) {
        try {
            Path dir = CustomIconManager.ensureCustomIconDirectory(summary.getLevelId());
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) { Desktop.getDesktop().open(dir.toFile()); return; }
            } catch (Exception ignored) {}
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) new ProcessBuilder("explorer.exe", dir.toAbsolutePath().toString()).start();
            else if (os.contains("mac")) new ProcessBuilder("open", dir.toAbsolutePath().toString()).start();
            else new ProcessBuilder("xdg-open", dir.toAbsolutePath().toString()).start();
        } catch (Exception e) { LOGGER.warn("Failed to open custom icon folder for world '{}'", summary.getLevelId(), e); }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float tick) {
        tryPreloadWorldIcons();
        CustomIconManager.processPendingRegistrations();

        if (isPanelMixed()) { boolean shouldShow = hasWorldSelected(); if (panelVisible != shouldShow) panelVisible = shouldShow; }
        if (isPanelDynamic() || isPanelMixed()) {
            float targetProgress = panelVisible ? 1.0f : 0.0f;
            if (Math.abs(panelAnimationProgress - targetProgress) > 0.001f) {
                panelAnimationProgress = Mth.lerp((float) Config.PANEL_ANIMATION_SPEED.get().doubleValue(), panelAnimationProgress, targetProgress);
                updateLayout();
            } else panelAnimationProgress = targetProgress;
        }

        int laLeft = listAreaLeft(), laWidth = listAreaWidth();
        int desiredCenter = laLeft + laWidth / 2;
        int originalWidth = this.width;
        this.width = isPanelLeft() ? desiredCenter * 2 : laWidth;
        super.render(gui, mouseX, mouseY, tick);
        this.width = originalWidth;

        if (panelAnimationProgress > 0.001f) {
            int alpha = Config.PANEL_ALPHA.get();
            int bgColor = Config.PANEL_BACKGROUND_STYLE.get() == Config.BackgroundStyle.GRAY ? 0x1A1A1A : 0x000000;
            int pL = panelLeft(), pR = panelRight();
            gui.fill(pL, 0, pR, this.height, (alpha << 24) | bgColor);
            if (isPanelLeft()) gui.fill(pR - 1, 0, pR, this.height, 0x66555555);
            else gui.fill(pL, 0, pL + 1, this.height, 0x66555555);
            this.sidebarScroll = Mth.lerp(0.2f, this.sidebarScroll, this.sidebarTargetScroll);
            renderSidebarInfo(gui);
        }

        if (isPanelDynamic()) renderToggleButton(gui, mouseX, mouseY);
        if (this.cachedList != null && this.cachedList.children().isEmpty()) {
            GuiCompat.drawCenteredString(gui, this.font, Component.translatable("reimagined_world_select.no_worlds"), laLeft + laWidth / 2, this.height / 2, 0x808080);
        }
    }

    private void releaseCachedTexturesForWorld(String levelId) {
        if (this.minecraft == null) return;
        releaseCachedTexture(levelId);
        releaseCachedTexture("large_" + levelId);
        CustomIconManager.releaseWorld(levelId);
    }

    private void releaseCachedTexture(String key) {
        LoadedImage image = this.staticTextureCache.remove(key);
        if (image != null && !image.location().equals(DEFAULT_ICON)) this.minecraft.getTextureManager().release(image.location());
    }

    private void renderToggleButton(GuiGraphics gui, int mouseX, int mouseY) {
        int btnWidth = 12, btnHeight = 17;
        int btnY = (this.height - btnHeight) / 2;
        int btnX; String arrow;
        if (isPanelLeft()) { btnX = currentPanelPx(); arrow = panelVisible ? "<" : ">"; }
        else { btnX = panelLeft() - btnWidth; arrow = panelVisible ? ">" : "<"; }
        boolean hovered = mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight;
        gui.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, hovered ? 0x80404040 : 0x60303030);
        if (isPanelLeft()) gui.fill(btnX + btnWidth - 1, btnY, btnX + btnWidth, btnY + btnHeight, 0x66555555);
        else gui.fill(btnX, btnY, btnX + 1, btnY + btnHeight, 0x66555555);
        int textX = btnX + (btnWidth - this.font.width(arrow)) / 2;
        int textY = btnY + (btnHeight - this.font.lineHeight) / 2;
        GuiCompat.drawString(gui, this.font, arrow, textX, textY, hovered ? 0xFFFFFFFF : 0xFFCCCCCC, true);
    }

    private int getSidebarContentHeight() {
        LevelSummary selected = getSelectedSummary();
        if (selected == null) return 0;
        int y = 0, pW = this.panelWidth - 20;
        List<String> order = getOrder();
        for (String id : order) y = addHeightForElement(id, y, pW, selected);
        List<Config.CustomLineConfig> customLines = Config.getCustomLines();
        long customsInOrder = order.stream().filter(s -> s.startsWith("custom_")).count();
        int extraCustoms = customLines.size() - (int) customsInOrder;
        if (extraCustoms > 0) y += extraCustoms * singleInfoLineHeight();
        return y;
    }

    private int computeAlignmentOffset() {
        int contentHeight = getSidebarContentHeight();
        int visibleHeight = this.height - 20;
        return switch (Config.PANEL_TEXT_ALIGNMENT.get()) {
            case TOP -> 20;
            case CENTER -> Math.max(20, (visibleHeight - contentHeight) / 2 + 10);
            case BOTTOM -> Math.max(20, visibleHeight - contentHeight);
        };
    }

    private int addHeightForElement(String id, int y, int pW, LevelSummary s) {
        if (id.startsWith("custom_")) return y + singleInfoLineHeight();
        return switch (id) {
            case "large_icon" -> Config.SHOW_LARGE_ICON.get() ? y + computeScaledLargeIconBoxHeight(s, pW) + 12 : y;
            case "world_name" -> Config.SHOW_WORLD_NAME.get() ? y + scaledLineHeight() + 4 : y;
            case "folder_name" -> Config.SHOW_FOLDER_NAME.get() ? y + scaledLineHeight() + 8 : y;
            case "game_mode" -> Config.SHOW_GAME_MODE.get() ? y + singleInfoLineHeight() : y;
            case "difficulty" -> Config.SHOW_DIFFICULTY.get() ? y + singleInfoLineHeight() : y;
            case "time_played" -> Config.SHOW_TIME_PLAYED.get() ? y + singleInfoLineHeight() : y;
            case "version" -> Config.SHOW_VERSION.get() ? y + singleInfoLineHeight() : y;
            case "cheats" -> Config.SHOW_CHEATS.get() ? y + singleInfoLineHeight() : y;
            case "last_played" -> Config.SHOW_LAST_PLAYED.get() ? y + scaledLineHeight() + 5 : y;
            default -> y;
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> getOrder() {
        List<String> order = (List<String>) Config.PANEL_ORDER.get();
        if (order.isEmpty()) order = PanelOrderScreen.getDefaultOrder();
        return order;
    }

    private void renderSidebarInfo(GuiGraphics gui) {
        if (this.cachedList == null || !(this.cachedList.getSelected() instanceof WorldSelectionList.WorldListEntry entry)) return;
        if (panelAnimationProgress < 0.001f) return;
        Minecraft mc = Objects.requireNonNull(this.minecraft);
        int mouseX = (int) (mc.mouseHandler.xpos() * this.width / mc.getWindow().getScreenWidth());
        int mouseY = (int) (mc.mouseHandler.ypos() * this.height / mc.getWindow().getScreenHeight());
        LevelSummary s = ((WorldListEntryAccessor) (Object) entry).getSummary();
        int pL = panelLeft(), pR = panelRight(), cx = (pL + pR) / 2;
        int alignOffset = computeAlignmentOffset();
        int y = alignOffset - (int) this.sidebarScroll;
        int pW = this.panelWidth - 20;
        gui.enableScissor(pL, 0, pR, this.height);

        List<String> order = getOrder();
        List<Config.CustomLineConfig> customLines = Config.getCustomLines();
        Map<String, String> worldVars = RWSCustomInfoAPI.getWorldVariables(s.getLevelId());
        Map<String, String> playerVars = RWSCustomInfoAPI.getPlayerVariables(s.getLevelId());
        Set<Integer> renderedCustomIndices = new HashSet<>();

        for (String id : order) {
            if (id.startsWith("custom_")) {
                try {
                    int customIndex = Integer.parseInt(id.substring(7));
                    if (customIndex >= 0 && customIndex < customLines.size()) {
                        y = renderCustomAnimatedLine(gui, customLines.get(customIndex), worldVars, playerVars, s, cx, y, pW, mouseX, mouseY);
                        renderedCustomIndices.add(customIndex);
                    }
                } catch (NumberFormatException ignored) {}
            } else {
                y = renderElement(gui, id, s, cx, y, pW, mouseX, mouseY);
            }
        }
        for (int i = 0; i < customLines.size(); i++) {
            if (!renderedCustomIndices.contains(i)) y = renderCustomAnimatedLine(gui, customLines.get(i), worldVars, playerVars, s, cx, y, pW, mouseX, mouseY);
        }
        gui.disableScissor();
    }

    private String resolveGameMode(LevelSummary s) {
        if (s.isHardcore()) return Component.translatable("reimagined_world_select.value.hardcore").getString();
        return Component.translatable(switch (s.getGameMode()) {
            case SURVIVAL -> "reimagined_world_select.value.survival";
            case CREATIVE -> "reimagined_world_select.value.creative";
            case ADVENTURE -> "reimagined_world_select.value.adventure";
            case SPECTATOR -> "reimagined_world_select.value.spectator";
        }).getString();
    }

    private int renderElement(GuiGraphics gui, String id, LevelSummary s, int cx, int y, int pW, int mouseX, int mouseY) {
        return switch (id) {
            case "large_icon" -> Config.SHOW_LARGE_ICON.get() ? renderLargeIconElement(gui, s, cx, y, pW, mouseX, mouseY) : y;
            case "world_name" -> Config.SHOW_WORLD_NAME.get() ? drawScaled(gui, s.getLevelName(), cx, y, 0xFFFFFF, pW, true, false) + 2 : y;
            case "folder_name" -> Config.SHOW_FOLDER_NAME.get() ? drawScaled(gui, "(" + s.getLevelId() + ")", cx, y, 0x777777, pW, false, true) + 8 : y + 6;
            case "game_mode" -> Config.SHOW_GAME_MODE.get() ? renderAnimatedLine(gui, Component.translatable("reimagined_world_select.label.mode").getString(), resolveGameMode(s), cx, y, s.isHardcore() ? 0xFF5555 : 0xAAAAAA, pW, mouseX, mouseY) : y;
            case "difficulty" -> {
                if (!Config.SHOW_DIFFICULTY.get()) yield y;
                Path worldDir = Objects.requireNonNull(this.minecraft).getLevelSource().getBaseDir().resolve(s.getLevelId());
                WorldInfoHelper.DifficultyInfo di = WorldInfoHelper.readDifficulty(worldDir);
                if (di == null) yield y;
                DifficultyRenderData d = getDifficultyRenderData(di);
                yield renderAnimatedLine(gui, Component.translatable("reimagined_world_select.label.difficulty").getString(), d.text(), cx, y, d.color(), pW, mouseX, mouseY);
            }
            case "time_played" -> {
                if (!Config.SHOW_TIME_PLAYED.get()) yield y;
                Path worldDir = Objects.requireNonNull(this.minecraft).getLevelSource().getBaseDir().resolve(s.getLevelId());
                yield renderAnimatedLine(gui, Component.translatable("reimagined_world_select.label.time_played").getString(), formatPlayTime(WorldInfoHelper.readPlayerPlayTime(worldDir)), cx, y, pW, mouseX, mouseY);
            }
            case "version" -> Config.SHOW_VERSION.get() ? renderAnimatedLine(gui, Component.translatable("reimagined_world_select.label.version").getString(), s.levelVersion().minecraftVersionName(), cx, y, pW, mouseX, mouseY) : y;
            case "cheats" -> {
                if (!Config.SHOW_CHEATS.get()) yield y;
                Integer rc = ChatFormatting.RED.getColor();
                int cc = rc != null && s.hasCommands() ? rc : 0xAAAAAA;
                yield renderAnimatedLine(gui, Component.translatable("reimagined_world_select.label.commands").getString(), s.hasCommands() ? Component.translatable("reimagined_world_select.value.on").getString() : Component.translatable("reimagined_world_select.value.off").getString(), cx, y, cc, pW, mouseX, mouseY);
            }
            case "last_played" -> Config.SHOW_LAST_PLAYED.get() ? drawScaled(gui, "(" + dateFormat.format(new Date(s.getLastPlayed())) + ")", cx, y + 3, 0x777777, pW, false, true) + 5 : y;
            default -> y;
        };
    }

    private int computeScaledLargeIconBoxHeight(LevelSummary s, int pW) {
        float scale = (float) Config.LARGE_ICON_SCALE.get().doubleValue();
        int scaledWidth = Math.max(16, Math.round(pW * scale));
        return Math.max(32, switch (Config.LARGE_ICON_ASPECT_RATIO.get()) {
            case RATIO_16_9 -> scaledWidth * 9 / 16;
            case RATIO_4_3 -> scaledWidth * 3 / 4;
            case RATIO_1_1 -> scaledWidth;
        });
    }

    private void renderFadeTransition(GuiGraphics gui, CustomIconManager.IconLayer current, CustomIconManager.IconLayer next, int boxX, int boxY, int boxWidth, int boxHeight) {
        float progress = Math.max(0.0f, Math.min(1.0f, next.alpha()));
        int bgColor = Config.PANEL_BACKGROUND_STYLE.get() == Config.BackgroundStyle.GRAY ? 0x1A1A1A : 0x000000;

        if (progress <= 0.5f) {
            renderTexturedLayer(gui, current.texture(), current.width(), current.height(), boxX, boxY, boxWidth, boxHeight);
            float local = progress / 0.5f;
            int alpha = Math.max(0, Math.min(255, Math.round(local * 255.0f)));
            gui.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, (alpha << 24) | bgColor);
        } else {
            renderTexturedLayer(gui, next.texture(), next.width(), next.height(), boxX, boxY, boxWidth, boxHeight);
            float local = (1.0f - progress) / 0.5f;
            int alpha = Math.max(0, Math.min(255, Math.round(local * 255.0f)));
            gui.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, (alpha << 24) | bgColor);
        }
    }

    private int renderLargeIconElement(GuiGraphics gui, LevelSummary s, int cx, int y, int pW, int mouseX, int mouseY) {
        float scale = (float) Config.LARGE_ICON_SCALE.get().doubleValue();
        int scaledWidth = Math.max(16, Math.round(pW * scale));
        int boxHeight = computeScaledLargeIconBoxHeight(s, pW);
        int boxX = cx - scaledWidth / 2;

        gui.enableScissor(boxX, y, boxX + scaledWidth, y + boxHeight);

        CustomIconManager.DisplayedIcon custom = CustomIconManager.getDisplayedIcon(s.getLevelId());
        if (custom.isPresent()) {
            List<CustomIconManager.IconLayer> layers = custom.layers();
            if (layers.size() >= 2) {
                renderFadeTransition(gui, layers.get(0), layers.get(1), boxX, y, scaledWidth, boxHeight);
            } else {
                CustomIconManager.IconLayer layer = layers.getFirst();
                renderTexturedLayer(gui, layer.texture(), layer.width(), layer.height(), boxX, y, scaledWidth, boxHeight);
            }
        } else if (!CustomIconManager.hasAnyCustomIcon(s.getLevelId())) {
            LoadedImage fallback = loadFallbackLargeImage(s);
            renderTexturedLayer(gui, fallback.location(), fallback.width(), fallback.height(), boxX, y, scaledWidth, boxHeight);
        }

        gui.disableScissor();
        renderCustomIconFolderButton(gui, boxX, y, scaledWidth, boxHeight, mouseX, mouseY);
        return y + boxHeight + 12;
    }

    private void renderTexturedLayer(GuiGraphics gui, ResourceLocation texture, int srcW, int srcH, int boxX, int boxY, int boxW, int boxH) {
        if (srcW <= 0 || srcH <= 0) return;
        int dX, dY, dW, dH;
        switch (Config.LARGE_ICON_DISPLAY_MODE.get()) {
            case CROP -> {
                float cs = Math.max((float) boxW / srcW, (float) boxH / srcH);
                dW = Math.max(1, Math.round(srcW * cs));
                dH = Math.max(1, Math.round(srcH * cs));
                dX = boxX + (boxW - dW) / 2;
                dY = boxY + (boxH - dH) / 2;
            }
            default -> { dX = boxX; dY = boxY; dW = boxW; dH = boxH; }
        }
        GuiCompat.blit(gui, texture, dX, dY, 0.0f, 0.0f, dW, dH, dW, dH);
    }

    private void renderCustomIconFolderButton(GuiGraphics gui, int bx, int by, int bw, int bh, int mx, int my) {
        Rect r = new Rect(bx + bw - 16, by + 4, 12, 12);
        boolean h = r.contains(mx, my);
        gui.fill(r.x(), r.y(), r.x() + r.width(), r.y() + r.height(), h ? 0xC0303030 : 0x90202020);
        GuiCompat.drawCenteredString(gui, this.font, "+", r.x() + r.width() / 2, r.y() + 2, h ? 0xFFFFFF : 0xDDDDDD);
    }

    @Nullable
    private Rect getLargeIconRect(LevelSummary summary) {
        if (!Config.SHOW_LARGE_ICON.get() || panelAnimationProgress < 0.001f) return null;
        int cx = (panelLeft() + panelRight()) / 2;
        int y = computeAlignmentOffset() - (int) this.sidebarScroll;
        int pW = this.panelWidth - 20;
        for (String id : getOrder()) {
            if ("large_icon".equals(id)) {
                float sc = (float) Config.LARGE_ICON_SCALE.get().doubleValue();
                int sw = Math.max(16, Math.round(pW * sc));
                return new Rect(cx - sw / 2, y, sw, computeScaledLargeIconBoxHeight(summary, pW));
            }
            y = addHeightForElement(id, y, pW, summary);
        }
        return null;
    }

    @Nullable
    private Rect getLargeIconButtonRect() {
        LevelSummary s = getSelectedSummary();
        if (s == null) return null;
        Rect r = getLargeIconRect(s);
        if (r == null) return null;
        return new Rect(r.x() + r.width() - 16, r.y() + 4, 12, 12);
    }

    private int renderCustomAnimatedLine(GuiGraphics gui, Config.CustomLineConfig line, Map<String, String> wv, Map<String, String> pv, LevelSummary s, int cx, int y, int mw, int mx, int my) {
        if (line.isEmpty()) return y;
        Map<String, String> sv = line.source() == Config.VariableSource.PLAYER ? pv : wv;
        String raw = sv.get("var" + line.varIndex());
        String iv = interpretVar(raw, line.mode());
        String label = line.label();
        if (line.hasLangLabel()) label = label.replace(line.getLangLabelPlaceholder(), Component.translatable(line.getLangLabelKey()).getString());
        String format = line.format();
        if (line.hasLangFormat()) format = format.replace(line.getLangFormatPlaceholder(), Component.translatable(line.getLangFormatKey()).getString());
        String pv2 = parsePlaceholders(format, s).replace("%var%", iv);
        if (!label.isEmpty() && !label.endsWith(" ")) label += ": ";
        return renderAnimatedLine(gui, label, pv2, cx, y, mw, mx, my);
    }

    private String interpretVar(@Nullable String raw, Config.VarMode mode) {
        if (mode == Config.VarMode.VALUE) return raw == null ? "" : raw;
        double n = 0;
        try { if (raw != null && !raw.isEmpty()) n = Double.parseDouble(raw); } catch (NumberFormatException ignored) {}
        return switch (mode) {
            case BOOLEAN -> n >= 1 ? Component.translatable("reimagined_world_select.value.true").getString() : Component.translatable("reimagined_world_select.value.false").getString();
            case ON_OFF -> n >= 1 ? Component.translatable("reimagined_world_select.value.on").getString() : Component.translatable("reimagined_world_select.value.off").getString();
            default -> raw == null ? "" : raw;
        };
    }

    private String parsePlaceholders(String t, LevelSummary s) {
        if (t.isEmpty()) return "";
        return t.replace("%name%", s.getLevelName()).replace("%id%", s.getLevelId()).replace("%mode%", resolveGameMode(s))
                .replace("%version%", s.levelVersion().minecraftVersionName())
                .replace("%cheats%", s.hasCommands() ? Component.translatable("reimagined_world_select.value.on").getString() : Component.translatable("reimagined_world_select.value.off").getString())
                .replace("%hardcore%", s.isHardcore() ? Component.translatable("reimagined_world_select.value.yes").getString() : Component.translatable("reimagined_world_select.value.no").getString());
    }

    private String formatPlayTime(long ticks) {
        double m = ticks / 20.0 / 60.0; double v; String u;
        switch (Config.TIME_PLAYED_FORMAT.get()) {
            case MINUTES -> { v = m; u = Component.translatable("reimagined_world_select.time.minutes").getString(); }
            case HOURS -> { v = m / 60; u = Component.translatable("reimagined_world_select.time.hours").getString(); }
            case DAYS -> { v = m / 1440; u = Component.translatable("reimagined_world_select.time.days").getString(); }
            case WEEKS -> { v = m / 10080; u = Component.translatable("reimagined_world_select.time.weeks").getString(); }
            case MONTHS -> { v = m / 43200; u = Component.translatable("reimagined_world_select.time.months").getString(); }
            case YEARS -> { v = m / 525600; u = Component.translatable("reimagined_world_select.time.years").getString(); }
            default -> throw new IllegalStateException();
        }
        String f = v >= 100 ? String.format(Locale.ROOT, "%.0f", v) : String.format(Locale.ROOT, "%.1f", v);
        if (f.endsWith(".0")) f = f.substring(0, f.length() - 2);
        return f + u;
    }

    private int drawScaled(GuiGraphics g, String text, int x, int y, int color, int mw, boolean bold, boolean italic) {
        FormattedCharSequence seq = Component.literal(text).withStyle(Style.EMPTY.withBold(bold).withItalic(italic)).getVisualOrderText();
        float tw = this.font.width(seq); float sc = panelTextScale();
        if (tw > 0 && tw * sc > mw) sc = (float) mw / tw;
        PoseStack p = GuiCompat.pose(g); p.pushPose();
        p.translate(x, y + (this.font.lineHeight * (1 - sc) / 2f), 0); p.scale(sc, sc, 1);
        GuiCompat.drawString(g, this.font, seq, (int) (-tw / 2), 0, color, true);
        p.popPose();
        return y + (int) (this.font.lineHeight * sc) + 2;
    }

    private int renderAnimatedLine(GuiGraphics g, String l, String v, int cx, int y, int mw, int mx, int my) { return renderAnimatedLine(g, l, v, cx, y, 0xAAAAAA, mw, mx, my); }

    private int renderAnimatedLine(GuiGraphics g, String l, String v, int cx, int y, int vc, int mw, int mx, int my) {
        float tw = this.font.width(l + v); float bs = panelTextScale();
        if (tw > 0 && tw * bs > mw) bs = (float) mw / tw;
        boolean h = mx >= panelLeft() && mx <= panelRight() && my >= y - 2 && my <= y + scaledLineHeight() + 2;
        String ak = l + "|" + v + "|" + y;
        float ca = sidebarLineAnimations.getOrDefault(ak, 0f);
        float ta = Config.ENABLE_TEXT_HOVER_ANIMATION.get() && h ? 1f : 0f;
        ca = Mth.lerp(0.18f, ca, ta); if (Math.abs(ca - ta) < 0.01f) ca = ta;
        sidebarLineAnimations.put(ak, ca);
        float fs = bs * Mth.lerp(ca, 1f, 1.08f);
        if (tw > 0 && tw * fs > mw) fs = (float) mw / tw;
        PoseStack p = GuiCompat.pose(g); p.pushPose(); p.translate(cx, y, 0); p.scale(fs, fs, 1);
        int sx = (int) (-tw / 2);
        GuiCompat.drawString(g, this.font, l, sx, 0, 0x555555, false);
        GuiCompat.drawString(g, this.font, v, sx + this.font.width(l), 0, vc, false);
        p.popPose();
        return y + (int) (this.font.lineHeight * fs) + 2;
    }

    @Nonnull public ResourceLocation loadIcon(LevelSummary s) { return loadSmallIconImage(s).location(); }
    @Nonnull public ResourceLocation loadLargeIcon(LevelSummary s) {
        CustomIconManager.DisplayedIcon c = CustomIconManager.getDisplayedIcon(s.getLevelId());
        return c.isPresent() ? c.layers().getLast().texture() : loadFallbackLargeImage(s).location();
    }
    @Nonnull private LoadedImage loadSmallIconImage(LevelSummary s) { return loadStaticImage(s, false); }
    @Nonnull private LoadedImage loadFallbackLargeImage(LevelSummary s) { return loadStaticImage(s, true); }

    @Nonnull
    private LoadedImage loadStaticImage(LevelSummary s, boolean large) {
        String id = s.getLevelId(); String ck = (large ? "large_" : "") + id;
        LoadedImage c = staticTextureCache.get(ck); if (c != null) return c;
        Path wd = Objects.requireNonNull(minecraft).getLevelSource().getBaseDir().resolve(id);
        Path tp;
        if (large) { Path cl = wd.resolve("custom_large_icon.png"); Path l = wd.resolve("large_icon.png"); Path n = wd.resolve("icon.png"); tp = Files.exists(cl) ? cl : Files.exists(l) ? l : n; }
        else tp = wd.resolve("icon.png");
        if (!Files.exists(tp)) return DEFAULT_LOADED_ICON;
        try (InputStream is = Files.newInputStream(tp)) {
            NativeImage img = NativeImage.read(is); int w = img.getWidth(), h = img.getHeight();
            DynamicTexture t = TextureCompat.createFromImage(img);
            String si = id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./\\-]", "_").replaceAll("_+", "_");
            long st = Files.getLastModifiedTime(tp).toMillis(); long ha = (st ^ tp.toString().hashCode()) & 0xFFFFFFFFL;
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("reimagined_world_select", (large ? "rws_large_" : "rws_icon_") + si + "_" + ha);
            Objects.requireNonNull(minecraft).getTextureManager().register(rl, t);
            LoadedImage li = new LoadedImage(rl, w, h); staticTextureCache.put(ck, li); return li;
        } catch (Exception e) { LOGGER.warn("Failed to load image for '{}': {}", id, e.getMessage()); return DEFAULT_LOADED_ICON; }
    }

    private DifficultyRenderData getDifficultyRenderData(WorldInfoHelper.DifficultyInfo di) {
        String t; int c;
        switch (di.difficultyId()) {
            case 0 -> { t = Component.translatable("reimagined_world_select.value.peaceful").getString(); c = 0xFFFFFF; }
            case 1 -> { t = Component.translatable("reimagined_world_select.value.easy").getString(); c = 0x55FF55; }
            case 2 -> { t = Component.translatable("reimagined_world_select.value.normal").getString(); c = 0xFFFF55; }
            case 3 -> { t = Component.translatable("reimagined_world_select.value.hard").getString(); c = 0xFF5555; }
            default -> { t = Component.translatable("reimagined_world_select.value.unknown").getString(); c = 0xAAAAAA; }
        }
        if (di.locked()) { t += " " + Component.translatable("reimagined_world_select.value.locked").getString(); c = 0x555555; }
        return new DifficultyRenderData(t, c);
    }

    @Override public void onClose() {
        for (LoadedImage i : staticTextureCache.values()) { if (!i.location().equals(DEFAULT_ICON) && minecraft != null) minecraft.getTextureManager().release(i.location()); }
        staticTextureCache.clear(); sidebarLineAnimations.clear(); RWSCustomInfoAPI.clearCache(); CustomIconManager.clear(); super.onClose();
    }

    private record DifficultyRenderData(String text, int color) {}
    private record LoadedImage(ResourceLocation location, int width, int height) {}
    private record Rect(int x, int y, int width, int height) {
        boolean contains(double mx, double my) { return mx >= x && mx <= x + width && my >= y && my <= y + height; }
    }
}