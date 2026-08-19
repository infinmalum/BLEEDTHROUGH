package com.bleedthrough.meatscape.safety;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Public compatibility seam for bulk movers and optional mod integrations. */
public final class ProvenanceService {
    private ProvenanceService() { }

    public static void markModified(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.is(MeatscapeBlockTags.NATURAL_REPLACEABLE) || !level.hasChunkAt(pos)) return;
        var data = ChunkSafetyService.get(level, pos);
        data.markModified(pos);
        if (data.trust() == TerrainTrust.TRUSTED) data.setTrust(TerrainTrust.PLAYER_MODIFIED);
    }

    public static void markUntrusted(ServerLevel level, BlockPos min, BlockPos max) {
        BlockPos low = new BlockPos(Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()));
        BlockPos high = new BlockPos(Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ()));
        for (BlockPos cursor : BlockPos.betweenClosed(low, high)) {
            BlockPos pos = cursor.immutable();
            if (level.hasChunkAt(pos)) markModified(level, pos, level.getBlockState(pos));
        }
    }
}
