package com.inf2z.reimagined_world_selection;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.ArrayList;
import java.util.List;

public class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue PANEL_WIDTH_RATIO;
    public static final ModConfigSpec.EnumValue<BackgroundStyle> PANEL_BACKGROUND_STYLE;
    public static final ModConfigSpec.IntValue PANEL_ALPHA;
    public static final ModConfigSpec.EnumValue<PanelBehaviour> PANEL_BEHAVIOUR;

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

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CUSTOM_LINES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PANEL_ORDER;

    public enum BackgroundStyle { GRAY, BLACK }
    public enum VarMode { BOOLEAN, ON_OFF, VALUE }
    public enum TimeFormat { MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS }
    public enum VariableSource { WORLD, PLAYER }
    public enum PanelBehaviour { DYNAMIC, STATIC }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Panel Settings");
        builder.push("panel_settings");

        PANEL_WIDTH_RATIO = builder
                .comment("Width ratio of the side panel (0.2 - 0.6)")
                .defineInRange("panel_width_ratio", 0.4, 0.2, 0.6);

        PANEL_BACKGROUND_STYLE = builder
                .comment("Background style: GRAY or BLACK")
                .defineEnum("panel_background_style", BackgroundStyle.BLACK);

        PANEL_ALPHA = builder
                .comment("Background transparency (0-255)")
                .defineInRange("panel_alpha", 102, 0, 255);

        PANEL_BEHAVIOUR = builder
                .comment("Panel behavior: DYNAMIC (with toggle button) or STATIC (always visible)")
                .defineEnum("panel_behaviour", PanelBehaviour.DYNAMIC);

        builder.pop();

        builder.comment("Element Animations");
        builder.push("element_animations");

        ENABLE_WORLD_HOVER_ANIMATION = builder
                .comment("Enable world selection hover animation")
                .define("enable_world_hover_animation", true);

        ENABLE_TEXT_HOVER_ANIMATION = builder
                .comment("Enable panel text hover animation")
                .define("enable_text_hover_animation", true);

        builder.pop();

        builder.comment("Visible Information");
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

        builder.comment("Internal Settings (managed by in-game editors)");
        builder.push("internal");

        CUSTOM_LINES = builder
                .comment("Custom lines data - use in-game Custom Lines Editor")
                .defineList("custom_lines", new ArrayList<>(), obj -> obj instanceof String);

        PANEL_ORDER = builder
                .comment("Panel element order - use in-game Panel Order Editor")
                .defineList("panel_order", new ArrayList<>(), obj -> obj instanceof String);

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
                try {
                    mode = VarMode.valueOf(parts[2].toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            VariableSource source = VariableSource.WORLD;
            if (parts.length > 3) {
                try {
                    source = VariableSource.valueOf(parts[3].toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            return new CustomLineConfig(label, format, mode, source, index + 1);
        }

        public boolean isEmpty() {
            return label.isEmpty() && format.isEmpty();
        }
    }

    public static List<CustomLineConfig> getCustomLines() {
        List<CustomLineConfig> lines = new ArrayList<>();
        List<? extends String> data = CUSTOM_LINES.get();

        for (int i = 0; i < data.size(); i++) {
            CustomLineConfig config = CustomLineConfig.parse(data.get(i), i);
            if (!config.isEmpty()) {
                lines.add(config);
            }
        }

        return lines;
    }
}