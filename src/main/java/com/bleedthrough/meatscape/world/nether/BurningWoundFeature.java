package com.bleedthrough.meatscape.world.nether;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Worldgen-only, bounded scan for a native Netherrack floor in the generating center chunk. */
public final class BurningWoundFeature extends Feature<NoneFeatureConfiguration> {
    public BurningWoundFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!(context.level() instanceof WorldGenRegion region) || !context.level().getLevel().dimension().equals(Level.NETHER)) return false;
        BlockPos origin = context.origin();
        if (!region.getCenter().equals(new net.minecraft.world.level.ChunkPos(origin))) return false;
        for (int offset = 0; offset <= 16; offset++) for (int sign : new int[] {1, -1}) {
            BlockPos pos = origin.offset(0, offset * sign, 0);
            if (context.level().isOutsideBuildHeight(pos) || !context.level().isEmptyBlock(pos)
                    || !context.level().getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.NETHERRACK)) continue;
            if (!context.level().setBlock(pos, MeatscapeBlocks.BURNING_WOUND.get().defaultBlockState(), Block.UPDATE_CLIENTS)) return false;
            context.level().setBlock(pos.north(), MeatscapeBlocks.CHARRED_SCAR.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            return true;
        }
        return false;
    }
}
