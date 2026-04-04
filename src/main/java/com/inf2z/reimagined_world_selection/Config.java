package com.inf2z.reimagined_world_selection;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue PANEL_WIDTH_RATIO;
    public static final ModConfigSpec.EnumValue<BackgroundStyle> PANEL_BACKGROUND_STYLE;
    public static final ModConfigSpec.IntValue PANEL_ALPHA;

    public static final ModConfigSpec.BooleanValue SHOW_WORLD_NAME;
    public static final ModConfigSpec.BooleanValue SHOW_FOLDER_NAME;
    public static final ModConfigSpec.BooleanValue SHOW_GAME_MODE;
    public static final ModConfigSpec.BooleanValue SHOW_VERSION;
    public static final ModConfigSpec.BooleanValue SHOW_CHEATS;
    public static final ModConfigSpec.BooleanValue SHOW_LAST_PLAYED;

    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_1_LABEL;
    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_1_FORMAT;
    public static final ModConfigSpec.EnumValue<VarMode> CUSTOM_LINE_1_MODE;

    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_2_LABEL;
    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_2_FORMAT;
    public static final ModConfigSpec.EnumValue<VarMode> CUSTOM_LINE_2_MODE;

    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_3_LABEL;
    public static final ModConfigSpec.ConfigValue<String> CUSTOM_LINE_3_FORMAT;
    public static final ModConfigSpec.EnumValue<VarMode> CUSTOM_LINE_3_MODE;

    public enum BackgroundStyle { GRAY, BLACK }

    public enum VarMode { BOOLEAN, ON_OFF, VALUE }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.translation("config.reimagined_world_selection.category.panel_settings");
        builder.push("Panel Settings");

        PANEL_WIDTH_RATIO = builder.comment("Width ratio of the side panel (0.2 to 0.6)")
                .translation("config.reimagined_world_selection.panel_width_ratio")
                .defineInRange("panelWidthRatio", 0.4, 0.2, 0.6);

        PANEL_BACKGROUND_STYLE = builder.comment("Background style of the panel")
                .translation("config.reimagined_world_selection.panel_background_style")
                .defineEnum("backgroundStyle", BackgroundStyle.BLACK);

        PANEL_ALPHA = builder.comment("Transparency of the panel background (0 = fully transparent, 255 = fully opaque)")
                .translation("config.reimagined_world_selection.panel_alpha")
                .defineInRange("panelAlpha", 102, 0, 255);
        builder.pop();

        builder.translation("config.reimagined_world_selection.category.visible_information");
        builder.push("Visible Information");

        SHOW_WORLD_NAME = builder.translation("config.reimagined_world_selection.show_world_name").define("showWorldName", true);
        SHOW_FOLDER_NAME = builder.translation("config.reimagined_world_selection.show_folder_name").define("showFolderName", true);
        SHOW_GAME_MODE = builder.translation("config.reimagined_world_selection.show_game_mode").define("showGameMode", true);
        SHOW_VERSION = builder.translation("config.reimagined_world_selection.show_version").define("showVersion", true);
        SHOW_CHEATS = builder.translation("config.reimagined_world_selection.show_cheats").define("showCheats", true);
        SHOW_LAST_PLAYED = builder.translation("config.reimagined_world_selection.show_last_played").define("showLastPlayed", true);
        builder.pop();

        builder.translation("config.reimagined_world_selection.category.custom_information_lines");
        builder.comment("Use %var% to insert the KubeJS variable. Other placeholders: %name%, %id%, %mode%, %version%, %cheats%, %hardcore%");
        builder.push("Custom Information Lines");

        CUSTOM_LINE_1_LABEL = builder.translation("config.reimagined_world_selection.custom_line_1_label").define("customLine1Label", "");
        CUSTOM_LINE_1_FORMAT = builder.translation("config.reimagined_world_selection.custom_line_1_format").define("customLine1Format", "");
        CUSTOM_LINE_1_MODE = builder.translation("config.reimagined_world_selection.custom_line_1_mode").defineEnum("customLine1Mode", VarMode.VALUE);

        CUSTOM_LINE_2_LABEL = builder.translation("config.reimagined_world_selection.custom_line_2_label").define("customLine2Label", "");
        CUSTOM_LINE_2_FORMAT = builder.translation("config.reimagined_world_selection.custom_line_2_format").define("customLine2Format", "");
        CUSTOM_LINE_2_MODE = builder.translation("config.reimagined_world_selection.custom_line_2_mode").defineEnum("customLine2Mode", VarMode.VALUE);

        CUSTOM_LINE_3_LABEL = builder.translation("config.reimagined_world_selection.custom_line_3_label").define("customLine3Label", "");
        CUSTOM_LINE_3_FORMAT = builder.translation("config.reimagined_world_selection.custom_line_3_format").define("customLine3Format", "");
        CUSTOM_LINE_3_MODE = builder.translation("config.reimagined_world_selection.custom_line_3_mode").defineEnum("customLine3Mode", VarMode.VALUE);
        builder.pop();

        SPEC = builder.build();
    }
}