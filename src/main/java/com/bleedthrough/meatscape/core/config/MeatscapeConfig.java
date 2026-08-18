package com.bleedthrough.meatscape.core.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common configuration owned by the core mod. */
public final class MeatscapeConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Enable additional Meatscape diagnostic logging.")
            .define("debugLogging", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private MeatscapeConfig() {
    }
}
