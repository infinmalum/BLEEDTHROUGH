package com.bleedthrough.meatscape.core.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common configuration owned by the core mod. */
public final class MeatscapeConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Enable additional Meatscape diagnostic logging.")
            .define("debugLogging", false);

    public static final ForgeConfigSpec.IntValue EVOLUTION_GLOBAL_BUDGET = BUILDER
            .comment("Maximum candidate positions selected by the Evolution Scheduler per server tick.")
            .defineInRange("evolution.globalTickBudget", 64, 1, 4096);

    public static final ForgeConfigSpec.IntValue EVOLUTION_PER_RIFT_BUDGET = BUILDER
            .comment("Maximum candidate positions selected for one Rift per server tick.")
            .defineInRange("evolution.perRiftTickBudget", 8, 1, 1024);

    public static final ForgeConfigSpec.BooleanValue CREATE_BULK_MOVEMENT_SAFETY = BUILDER
            .comment("Enable the dependency-free provenance hook intended for optional Create moving structures.")
            .define("integrations.createBulkMovementSafety", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private MeatscapeConfig() {
    }
}
