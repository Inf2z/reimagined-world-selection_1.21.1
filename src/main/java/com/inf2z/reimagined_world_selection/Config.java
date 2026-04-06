package com.inf2z.reimagined_world_selection;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue PANEL_WIDTH_RATIO;
    public static final ModConfigSpec.EnumValue<BackgroundStyle> PANEL_BACKGROUND_STYLE;
    public static final ModConfigSpec.IntValue PANEL_ALPHA;

    public static final ModConfigSpec.BooleanValue ENABLE_WORLD_HOVER_ANIMATION;
    public static final ModConfigSpec.BooleanValue ENABLE_TEXT_HOVER_ANIMATION;

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

    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_1_LABEL;
    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_1_FORMAT;
    public static final ModConfigSpec.EnumValue<VarMode> CUSTOM_LINE_1_MODE;
    public static final ModConfigSpec.EnumValue<VariableSource> CUSTOM_LINE_1_SOURCE;

    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_2_LABEL;
    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_2_FORMAT;
    public static final ModConfigSpec.EnumValue<VarMode> CUSTOM_LINE_2_MODE;
    public static final ModConfigSpec.EnumValue<VariableSource> CUSTOM_LINE_2_SOURCE;

    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_3_LABEL;
    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_3_FORMAT;
    public static final ModConfigSpec.EnumValue<VarMode> CUSTOM_LINE_3_MODE;
    public static final ModConfigSpec.EnumValue<VariableSource> CUSTOM_LINE_3_SOURCE;

    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_4_LABEL;
    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_4_FORMAT;
    public static final ModConfigSpec.EnumValue<VarMode> CUSTOM_LINE_4_MODE;
    public static final ModConfigSpec.EnumValue<VariableSource> CUSTOM_LINE_4_SOURCE;

    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_5_LABEL;
    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_5_FORMAT;
    public static final ModConfigSpec.EnumValue<VarMode> CUSTOM_LINE_5_MODE;
    public static final ModConfigSpec.EnumValue<VariableSource> CUSTOM_LINE_5_SOURCE;

    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_6_LABEL;
    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_6_FORMAT;
    public static final ModConfigSpec.EnumValue<VarMode> CUSTOM_LINE_6_MODE;
    public static final ModConfigSpec.EnumValue<VariableSource> CUSTOM_LINE_6_SOURCE;

    public enum BackgroundStyle { GRAY, BLACK }
    public enum VarMode { BOOLEAN, ON_OFF, VALUE }
    public enum TimeFormat { MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS }
    public enum VariableSource { WORLD, PLAYER }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.translation("config.reimagined_world_selection.category.panel_settings");
        builder.push("Panel Settings");

        PANEL_WIDTH_RATIO = builder.translation("config.reimagined_world_selection.panel_width_ratio")
                .defineInRange("panelWidthRatio", 0.4, 0.2, 0.6);

        PANEL_BACKGROUND_STYLE = builder.translation("config.reimagined_world_selection.panel_background_style")
                .defineEnum("backgroundStyle", BackgroundStyle.BLACK);

        PANEL_ALPHA = builder.translation("config.reimagined_world_selection.panel_alpha")
                .defineInRange("panelAlpha", 102, 0, 255);

        builder.pop();

        builder.translation("config.reimagined_world_selection.category.element_animations");
        builder.push("Element Animations");

        ENABLE_WORLD_HOVER_ANIMATION = builder.translation("config.reimagined_world_selection.enable_world_hover_animation")
                .define("enableWorldHoverAnimation", true);

        ENABLE_TEXT_HOVER_ANIMATION = builder.translation("config.reimagined_world_selection.enable_text_hover_animation")
                .define("enableTextHoverAnimation", true);

        builder.pop();

        builder.translation("config.reimagined_world_selection.category.visible_information");
        builder.push("Visible Information");

        SHOW_WORLD_NAME = builder.translation("config.reimagined_world_selection.show_world_name").define("showWorldName", true);
        SHOW_FOLDER_NAME = builder.translation("config.reimagined_world_selection.show_folder_name").define("showFolderName", true);
        SHOW_GAME_MODE = builder.translation("config.reimagined_world_selection.show_game_mode").define("showGameMode", true);
        SHOW_DIFFICULTY = builder.translation("config.reimagined_world_selection.show_difficulty").define("showDifficulty", true);
        SHOW_TIME_PLAYED = builder.translation("config.reimagined_world_selection.show_time_played").define("showTimePlayed", true);
        TIME_PLAYED_FORMAT = builder.translation("config.reimagined_world_selection.time_played_format").defineEnum("timePlayedFormat", TimeFormat.HOURS);
        SHOW_VERSION = builder.translation("config.reimagined_world_selection.show_version").define("showVersion", true);
        SHOW_CHEATS = builder.translation("config.reimagined_world_selection.show_cheats").define("showCheats", true);
        SHOW_LAST_PLAYED = builder.translation("config.reimagined_world_selection.show_last_played").define("showLastPlayed", true);
        SHOW_LARGE_ICON = builder.translation("config.reimagined_world_selection.show_large_icon").define("showLargeIcon", true);

        builder.pop();

        builder.translation("config.reimagined_world_selection.category.custom_information_lines");
        builder.push("Custom Information Lines");

        CUSTOM_LINE_1_LABEL = builder.translation("config.reimagined_world_selection.custom_line_1_label").define("customLine1Label", "");
        CUSTOM_LINE_1_FORMAT = builder.translation("config.reimagined_world_selection.custom_line_1_format").define("customLine1Format", "");
        CUSTOM_LINE_1_MODE = builder.translation("config.reimagined_world_selection.custom_line_1_mode").defineEnum("customLine1Mode", VarMode.VALUE);
        CUSTOM_LINE_1_SOURCE = builder.translation("config.reimagined_world_selection.custom_line_1_source").defineEnum("customLine1Source", VariableSource.WORLD);

        CUSTOM_LINE_2_LABEL = builder.translation("config.reimagined_world_selection.custom_line_2_label").define("customLine2Label", "");
        CUSTOM_LINE_2_FORMAT = builder.translation("config.reimagined_world_selection.custom_line_2_format").define("customLine2Format", "");
        CUSTOM_LINE_2_MODE = builder.translation("config.reimagined_world_selection.custom_line_2_mode").defineEnum("customLine2Mode", VarMode.VALUE);
        CUSTOM_LINE_2_SOURCE = builder.translation("config.reimagined_world_selection.custom_line_2_source").defineEnum("customLine2Source", VariableSource.WORLD);

        CUSTOM_LINE_3_LABEL = builder.translation("config.reimagined_world_selection.custom_line_3_label").define("customLine3Label", "");
        CUSTOM_LINE_3_FORMAT = builder.translation("config.reimagined_world_selection.custom_line_3_format").define("customLine3Format", "");
        CUSTOM_LINE_3_MODE = builder.translation("config.reimagined_world_selection.custom_line_3_mode").defineEnum("customLine3Mode", VarMode.VALUE);
        CUSTOM_LINE_3_SOURCE = builder.translation("config.reimagined_world_selection.custom_line_3_source").defineEnum("customLine3Source", VariableSource.WORLD);

        CUSTOM_LINE_4_LABEL = builder.translation("config.reimagined_world_selection.custom_line_4_label").define("customLine4Label", "");
        CUSTOM_LINE_4_FORMAT = builder.translation("config.reimagined_world_selection.custom_line_4_format").define("customLine4Format", "");
        CUSTOM_LINE_4_MODE = builder.translation("config.reimagined_world_selection.custom_line_4_mode").defineEnum("customLine4Mode", VarMode.VALUE);
        CUSTOM_LINE_4_SOURCE = builder.translation("config.reimagined_world_selection.custom_line_4_source").defineEnum("customLine4Source", VariableSource.WORLD);

        CUSTOM_LINE_5_LABEL = builder.translation("config.reimagined_world_selection.custom_line_5_label").define("customLine5Label", "");
        CUSTOM_LINE_5_FORMAT = builder.translation("config.reimagined_world_selection.custom_line_5_format").define("customLine5Format", "");
        CUSTOM_LINE_5_MODE = builder.translation("config.reimagined_world_selection.custom_line_5_mode").defineEnum("customLine5Mode", VarMode.VALUE);
        CUSTOM_LINE_5_SOURCE = builder.translation("config.reimagined_world_selection.custom_line_5_source").defineEnum("customLine5Source", VariableSource.WORLD);

        CUSTOM_LINE_6_LABEL = builder.translation("config.reimagined_world_selection.custom_line_6_label").define("customLine6Label", "");
        CUSTOM_LINE_6_FORMAT = builder.translation("config.reimagined_world_selection.custom_line_6_format").define("customLine6Format", "");
        CUSTOM_LINE_6_MODE = builder.translation("config.reimagined_world_selection.custom_line_6_mode").defineEnum("customLine6Mode", VarMode.VALUE);
        CUSTOM_LINE_6_SOURCE = builder.translation("config.reimagined_world_selection.custom_line_6_source").defineEnum("customLine6Source", VariableSource.WORLD);

        builder.pop();

        SPEC = builder.build();
    }
}