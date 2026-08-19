package com.bleedthrough.meatscape.safety;

import com.bleedthrough.meatscape.safety.data.ChunkSafetyCapability;
import com.bleedthrough.meatscape.safety.data.ChunkSafetyData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkSafetyService {
    private ChunkSafetyService() { }

    public static ChunkSafetyData get(LevelChunk chunk) {
        return chunk.getCapability(ChunkSafetyCapability.INSTANCE)
                .orElseThrow(() -> new IllegalStateException("Missing Meatscape chunk safety capability"));
    }

    public static ChunkSafetyData get(ServerLevel level, BlockPos pos) {
        return get(level.getChunkAt(pos));
    }

    public static void setTrust(ServerLevel level, ChunkPos pos, TerrainTrust trust) {
        get(level.getChunk(pos.x, pos.z)).setTrust(trust);
    }
}
