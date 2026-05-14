package com.inf2z.reimagined_world_select;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue PANEL_WIDTH_RATIO;
    public static final ModConfigSpec.EnumValue<BackgroundStyle> PANEL_BACKGROUND_STYLE;
    public static final ModConfigSpec.IntValue PANEL_ALPHA;
    public static final ModConfigSpec.EnumValue<PanelBehaviour> PANEL_BEHAVIOUR;
    public static final ModConfigSpec.EnumValue<PanelPosition> PANEL_POSITION;
    public static final ModConfigSpec.DoubleValue PANEL_TEXT_SCALE;
    public static final ModConfigSpec.EnumValue<PanelTextAlignment> PANEL_TEXT_ALIGNMENT;
    public static final ModConfigSpec.EnumValue<NoWorldsBehaviour> NO_WORLDS_BEHAVIOUR;

    public static final ModConfigSpec.BooleanValue ENABLE_WORLD_HOVER_ANIMATION;
    public static final ModConfigSpec.BooleanValue ENABLE_TEXT_HOVER_ANIMATION;
    public static final ModConfigSpec.DoubleValue PANEL_ANIMATION_SPEED;
    public static final ModConfigSpec.DoubleValue WORLD_SELECT_ANIMATION_SPEED;
    public static final ModConfigSpec.DoubleValue WORLD_DESELECT_ANIMATION_SPEED;

    public static final ModConfigSpec.BooleanValue SHOW_WORLD_NAME;
    public static final ModConfigSpec.BooleanValue SHOW_FOLDER_NAME;
    public static final ModConfigSpec.BooleanValue SHOW_GAME_MODE;
    public static final ModConfigSpec.BooleanValue SHOW_DIFFICULTY;
    public static final ModConfigSpec.BooleanValue SHOW_TIME_PLAYED;
    public static final ModConfigSpec.EnumValue<TimeFormat> TIME_PLAYED_FORMAT;
    public static final ModConfigSpec.BooleanValue SHOW_VERSION;
    public static final ModConfigSpec.BooleanValue SHOW_CHEATS;
    public static final ModConfigSpec.BooleanValue SHOW_LAST_PLAYED;
    public static final ModConfigSpec.BooleanValue SHOW_LARGE_ICON;

    public static final ModConfigSpec.EnumValue<LargeIconAspectRatio> LARGE_ICON_ASPECT_RATIO;
    public static final ModConfigSpec.EnumValue<LargeIconDisplayMode> LARGE_ICON_DISPLAY_MODE;
    public static final ModConfigSpec.DoubleValue LARGE_ICON_SCALE;
    public static final ModConfigSpec.BooleanValue CUSTOM_ICON_SLIDESHOW;
    public static final ModConfigSpec.IntValue CUSTOM_ICON_SLIDESHOW_INTERVAL_SECONDS;
    public static final ModConfigSpec.DoubleValue CUSTOM_ICON_SLIDESHOW_FADE_SECONDS;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CUSTOM_LINES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PANEL_ORDER;

    public enum BackgroundStyle { GRAY, BLACK }
    public enum VarMode { BOOLEAN, ON_OFF, VALUE }
    public enum TimeFormat { MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS }
    public enum VariableSource { WORLD, PLAYER }
    public enum PanelBehaviour { DYNAMIC, STATIC, MIXED }
    public enum PanelPosition { LEFT, RIGHT }
    public enum NoWorldsBehaviour { LIST, GENERATE }
    public enum LargeIconAspectRatio { RATIO_16_9, RATIO_4_3, RATIO_1_1 }
    public enum LargeIconDisplayMode { CROP, STRETCH }
    public enum PanelTextAlignment { TOP, CENTER, BOTTOM }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("panel_design");

        PANEL_WIDTH_RATIO = builder.defineInRange("panel_width_ratio", 0.4, 0.2, 0.6);
        PANEL_BACKGROUND_STYLE = builder.defineEnum("panel_background_style", BackgroundStyle.BLACK);
        PANEL_ALPHA = builder.defineInRange("panel_alpha", 102, 0, 255);
        PANEL_BEHAVIOUR = builder.defineEnum("panel_behaviour", PanelBehaviour.MIXED);
        PANEL_POSITION = builder.defineEnum("panel_position", PanelPosition.LEFT);
        PANEL_TEXT_SCALE = builder.defineInRange("panel_text_scale", 1.25, 0.5, 3.0);
        PANEL_TEXT_ALIGNMENT = builder
                .comment("TOP - content starts from top, CENTER - content centered vertically, BOTTOM - content at bottom")
                .defineEnum("panel_text_alignment", PanelTextAlignment.TOP);

        builder.pop();

        NO_WORLDS_BEHAVIOUR = builder.defineEnum("no_worlds_behaviour", NoWorldsBehaviour.LIST);

        builder.push("element_animations");

        ENABLE_WORLD_HOVER_ANIMATION = builder.define("enable_world_hover_animation", true);
        ENABLE_TEXT_HOVER_ANIMATION = builder.define("enable_text_hover_animation", true);
        PANEL_ANIMATION_SPEED = builder.defineInRange("panel_animation_speed", 0.15, 0.01, 1.0);
        WORLD_SELECT_ANIMATION_SPEED = builder.defineInRange("world_select_animation_speed", 0.14, 0.01, 1.0);
        WORLD_DESELECT_ANIMATION_SPEED = builder.defineInRange("world_deselect_animation_speed", 0.45, 0.01, 1.0);

        builder.pop();

        builder.push("visible_information");

        SHOW_WORLD_NAME = builder.define("show_world_name", true);
        SHOW_FOLDER_NAME = builder.define("show_folder_name", true);
        SHOW_GAME_MODE = builder.define("show_game_mode", true);
        SHOW_DIFFICULTY = builder.define("show_difficulty", true);
        SHOW_TIME_PLAYED = builder.define("show_time_played", true);
        TIME_PLAYED_FORMAT = builder.defineEnum("time_played_format", TimeFormat.HOURS);
        SHOW_VERSION = builder.define("show_version", true);
        SHOW_CHEATS = builder.define("show_cheats", true);
        SHOW_LAST_PLAYED = builder.define("show_last_played", true);
        SHOW_LARGE_ICON = builder.define("show_large_icon", true);

        builder.pop();

        builder.push("large_icon_settings");

        LARGE_ICON_ASPECT_RATIO = builder.defineEnum("large_icon_aspect_ratio", LargeIconAspectRatio.RATIO_4_3);
        LARGE_ICON_DISPLAY_MODE = builder
                .comment("CROP - crops image to fill the box, STRETCH - stretches image to fill the box")
                .defineEnum("large_icon_display_mode", LargeIconDisplayMode.CROP);
        LARGE_ICON_SCALE = builder.defineInRange("large_icon_scale", 1.0, 0.25, 3.0);
        CUSTOM_ICON_SLIDESHOW = builder.define("custom_icon_slideshow", true);
        CUSTOM_ICON_SLIDESHOW_INTERVAL_SECONDS = builder.defineInRange("custom_icon_slideshow_interval_seconds", 6, 1, 120);
        CUSTOM_ICON_SLIDESHOW_FADE_SECONDS = builder.defineInRange("custom_icon_slideshow_fade_seconds", 1.2, 0.1, 10.0);

        builder.pop();

        builder.push("internal");

        CUSTOM_LINES = builder.defineList("custom_lines", new ArrayList<>(), obj -> obj instanceof String);
        PANEL_ORDER = builder.defineList("panel_order", new ArrayList<>(), obj -> obj instanceof String);

        builder.pop();

        SPEC = builder.build();
    }

    public record CustomLineConfig(String label, String format, VarMode mode, VariableSource source, int varIndex) {
        public static CustomLineConfig parse(String data, int index) {
            String[] parts = data.split(";", -1);
            String label = parts.length > 0 ? parts[0] : "";
            String format = parts.length > 1 ? parts[1] : "";

            VarMode mode = VarMode.VALUE;
            if (parts.length > 2) {
                try { mode = VarMode.valueOf(parts[2].toUpperCase()); }
                catch (IllegalArgumentException ignored) {}
            }

            VariableSource source = VariableSource.WORLD;
            if (parts.length > 3) {
                try { source = VariableSource.valueOf(parts[3].toUpperCase()); }
                catch (IllegalArgumentException ignored) {}
            }

            return new CustomLineConfig(label, format, mode, source, index + 1);
        }

        public int lineNumber() { return varIndex; }
        public String getLangLabelPlaceholder() { return "%var" + lineNumber() + ".lang.label%"; }
        public String getLangFormatPlaceholder() { return "%var" + lineNumber() + ".lang.format%"; }
        public String getLangLabelKey() { return "rws.var" + lineNumber() + ".label"; }
        public String getLangFormatKey() { return "rws.var" + lineNumber() + ".format"; }
        public boolean hasLangLabel() { return label.contains(getLangLabelPlaceholder()); }
        public boolean hasLangFormat() { return format.contains(getLangFormatPlaceholder()); }
        public boolean isEmpty() { return label.isEmpty() && format.isEmpty(); }
    }

    public static List<CustomLineConfig> getCustomLines() {
        List<CustomLineConfig> lines = new ArrayList<>();
        List<? extends String> data = CUSTOM_LINES.get();
        for (int i = 0; i < data.size(); i++) {
            CustomLineConfig config = CustomLineConfig.parse(data.get(i), i);
            if (!config.isEmpty()) lines.add(config);
        }
        return lines;
    }
}