package com.bleedthrough.meatscape.core.registry;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.world.overworld.DormantRiftFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class MeatscapeFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, Meatscape.MOD_ID);
    static { FEATURES.register("dormant_rift", DormantRiftFeature::new); }
    private MeatscapeFeatures() { }
    public static void register(IEventBus bus) { FEATURES.register(bus); }
}
