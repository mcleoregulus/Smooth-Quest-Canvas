package com.leoregulus.smoothquestcanvas.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CanvasConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue SMOOTH_QUEST_PANNING = BUILDER
            .comment("Smooth FTB Quests canvas dragging and release inertia in browsing mode.",
                    "Editing, wheel/zoom and programmatic navigation keep their original behavior.")
            .define("smoothQuestPanning", true);
    public static final ModConfigSpec.DoubleValue QUEST_PAN_FOLLOW = BUILDER
            .comment("Drag-follow response per second. Higher values follow the pointer more closely.")
            .defineInRange("questPanFollow", 18.0, 8.0, 40.0);
    public static final ModConfigSpec.DoubleValue QUEST_PAN_FRICTION = BUILDER
            .comment("Release velocity decay per second. Higher values stop sooner.")
            .defineInRange("questPanFriction", 7.0, 2.0, 30.0);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private CanvasConfig() {}
}
