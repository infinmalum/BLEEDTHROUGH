package com.bleedthrough.meatscape.world.overworld;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.rift.RiftCoreBlock;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Writes only the generating chunk; no SavedData or server state is touched on worldgen workers. */
public final class DormantRiftFeature extends Feature<NoneFeatureConfiguration> {
    public static final TagKey<Block> SUBSTRATE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "dormant_rift_substrate"));

    public DormantRiftFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world = context.level();
        var pos = context.origin();
        if (!(world instanceof WorldGenRegion region) || !world.getLevel().dimension().equals(Level.OVERWORLD)) return false;
        if (!region.getCenter().equals(new net.minecraft.world.level.ChunkPos(pos))) return false;
        var chunk = region.getChunk(region.getCenter().x, region.getCenter().z);
        if (chunk.getStatus().isOrAfter(ChunkStatus.FULL) || world.isOutsideBuildHeight(pos)
                || world.isOutsideBuildHeight(pos.below()) || !world.isEmptyBlock(pos)
                || !world.getBlockState(pos.below()).is(SUBSTRATE)) return false;
        var core = MeatscapeBlocks.RIFT_CORE.get();
        if (!world.setBlock(pos, core.defaultBlockState().setValue(RiftCoreBlock.ACTIVE, false), Block.UPDATE_CLIENTS)) return false;
        // ProtoChunk serializes this tick. Registration happens on the server after it becomes ticking.
        world.scheduleTick(pos, core, 1);
        return true;
    }
}
