package com.bleedthrough.meatscape.core.registry;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.world.overworld.DormantRiftFeature;
import com.bleedthrough.meatscape.world.maw.MawNutrientMoundFeature;
import com.bleedthrough.meatscape.world.nether.BurningWoundFeature;
import com.bleedthrough.meatscape.world.end.EndWormholeFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class MeatscapeFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, Meatscape.MOD_ID);
    static {
        FEATURES.register("dormant_rift", DormantRiftFeature::new);
        FEATURES.register("maw_nutrient_mound", MawNutrientMoundFeature::new);
        FEATURES.register("burning_wound", BurningWoundFeature::new);
        FEATURES.register("end_wormhole", EndWormholeFeature::new);
    }
    private MeatscapeFeatures() { }
    public static void register(IEventBus bus) { FEATURES.register(bus); }
}
