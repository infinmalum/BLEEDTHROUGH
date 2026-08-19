package com.bleedthrough.meatscape.coherence.evolution;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Deterministic surface sampler used only to record future work positions. */
public final class CandidateSampler {
    private CandidateSampler() {
    }

    public static BlockPos sample(EvolutionTask task, long gameTime, EvolutionEnvironment environment) {
        long mixed = mix(task.riftId(), task.chunk().chunkPos(), gameTime);
        ChunkPos chunk = task.chunk().pos();
        int x = chunk.getMinBlockX() + (int) (mixed & 15L);
        int z = chunk.getMinBlockZ() + (int) ((mixed >>> 4) & 15L);
        return new BlockPos(x, environment.surfaceY(task.chunk(), x, z), z);
    }

    private static long mix(UUID id, long chunk, long gameTime) {
        long value = id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 17);
        value ^= Long.rotateLeft(chunk, 31) ^ gameTime * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
