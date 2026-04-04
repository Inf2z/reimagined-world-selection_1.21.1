package com.inf2z.reimagined_world_selection;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue PANEL_WIDTH_RATIO = BUILDER
            .translation("reimagined.config.panel_width_ratio")
            .defineInRange("panelWidthRatio", 0.4, 0.2, 0.6);

    public static final ModConfigSpec.BooleanValue SHOW_FOLDER_NAME = BUILDER
            .translation("reimagined.config.show_folder_name")
            .define("showFolderName", true);

    public static final ModConfigSpec.BooleanValue SHOW_CHEATS = BUILDER
            .translation("reimagined.config.show_cheats")
            .define("showCheats", true);

    public static final ModConfigSpec.IntValue PANEL_OPACITY = BUILDER
            .translation("reimagined.config.panel_opacity")
            .defineInRange("panelOpacity", 204, 0, 255);

    public static final ModConfigSpec.EnumValue<BackgroundStyle> PANEL_BACKGROUND_STYLE = BUILDER
            .translation("reimagined.config.panel_background_style")
            .defineEnum("panelBackgroundStyle", BackgroundStyle.BLACK);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public enum BackgroundStyle {
        BLACK,
        GRAY
    }
}