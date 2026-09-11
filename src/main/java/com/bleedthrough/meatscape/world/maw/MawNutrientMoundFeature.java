package com.bleedthrough.meatscape.world.maw;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** One safe surface resource per successful placement; no runtime growth or chunk loading. */
public final class MawNutrientMoundFeature extends Feature<NoneFeatureConfiguration> {
    public MawNutrientMoundFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var pos = context.origin();
        if (!level.getLevel().dimension().equals(MawDimensions.MAW) || !level.isEmptyBlock(pos)
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) return false;
        return level.setBlock(pos, MeatscapeBlocks.NUTRIENT_MOUND.get().defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}
