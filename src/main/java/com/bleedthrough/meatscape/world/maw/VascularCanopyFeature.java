package com.bleedthrough.meatscape.world.maw;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A small static canopy scene; it never spreads, ticks, or writes outside its generating chunk. */
public final class VascularCanopyFeature extends Feature<NoneFeatureConfiguration> {
    public VascularCanopyFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!(context.level() instanceof WorldGenRegion region) || !context.level().getLevel().dimension().equals(MawDimensions.MAW)) {
            return false;
        }
        BlockPos origin = context.origin();
        ChunkPos chunk = new ChunkPos(origin);
        if (!region.getCenter().equals(chunk) || !VascularCanopyLayout.fitsWithinChunk(origin, chunk)
                || !context.level().getBlockState(origin.below()).isFaceSturdy(context.level(), origin.below(), net.minecraft.core.Direction.UP)) {
            return false;
        }
        List<BlockPos> canopy = VascularCanopyLayout.blocks(origin);
        if (canopy.stream().anyMatch(pos -> !context.level().isEmptyBlock(pos))) {
            return false;
        }
        for (BlockPos pos : canopy) {
            context.level().setBlock(pos, MeatscapeBlocks.VASCULAR_MAT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

}
