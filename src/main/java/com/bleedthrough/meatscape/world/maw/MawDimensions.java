package com.bleedthrough.meatscape.world.maw;

import com.bleedthrough.meatscape.Meatscape;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

/** Stable keys for The Maw. Keep all cross-dimension callers behind this common-side class. */
public final class MawDimensions {
    public static final ResourceLocation MAW_ID = ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "maw");
    public static final ResourceKey<Level> MAW = ResourceKey.create(Registries.DIMENSION, MAW_ID);

    private MawDimensions() {
    }
}
