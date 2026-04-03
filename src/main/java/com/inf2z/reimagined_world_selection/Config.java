package com.inf2z.reimagined_world_selection;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.DoubleValue PANEL_WIDTH_RATIO;
    public static final ModConfigSpec.IntValue PANEL_OPACITY;
    public static final ModConfigSpec.BooleanValue SHOW_FOLDER_NAME;
    public static final ModConfigSpec.BooleanValue SHOW_CHEATS;
    public static final ModConfigSpec.EnumValue<BackgroundStyle> PANEL_BACKGROUND_STYLE;

    public enum BackgroundStyle { BLACK, GRAY }

    static {
        BUILDER.push("client");
        PANEL_WIDTH_RATIO = BUILDER.defineInRange("panelWidthRatio", 0.4, 0.1, 0.8);
        PANEL_OPACITY = BUILDER.defineInRange("panelOpacity", 204, 0, 255);
        PANEL_BACKGROUND_STYLE = BUILDER.defineEnum("panelBackgroundStyle", BackgroundStyle.BLACK);
        SHOW_FOLDER_NAME = BUILDER.define("showFolderName", true);
        SHOW_CHEATS = BUILDER.define("showCheats", true);
        BUILDER.pop();
    }
    public static final ModConfigSpec SPEC = BUILDER.build();
}