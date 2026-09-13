package com.bleedthrough.meatscape.world.end;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Generates only in Outer End chunks and writes only the active generation region. */
public final class EndWormholeFeature extends Feature<NoneFeatureConfiguration> {
    private static final long OUTER_END_RADIUS_SQUARED = 1_000_000L;
    public EndWormholeFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!(context.level() instanceof WorldGenRegion region) || !context.level().getLevel().dimension().equals(Level.END)) return false;
        BlockPos origin = context.origin();
        if (!region.getCenter().equals(new net.minecraft.world.level.ChunkPos(origin))) return false;
        long distanceSquared = (long) origin.getX() * origin.getX() + (long) origin.getZ() * origin.getZ();
        if (distanceSquared < OUTER_END_RADIUS_SQUARED) return false;
        for (int offset = 0; offset <= 16; offset++) for (int sign : new int[] {1, -1}) {
            BlockPos pos = origin.offset(0, offset * sign, 0);
            if (context.level().isOutsideBuildHeight(pos) || !context.level().isEmptyBlock(pos)
                    || !context.level().getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.END_STONE)) continue;
            return context.level().setBlock(pos, MeatscapeBlocks.END_WORMHOLE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return false;
    }
}
