package com.bleedthrough.meatscape.coherence.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class CandidateSamplerTest {
    @Test
    void sampleIsDeterministicAndInsideItsChunk() {
        ChunkPos chunk = new ChunkPos(-12, 34);
        EvolutionTask task = new EvolutionTask(
                UUID.fromString("68b1b5a1-c77f-4ef4-9804-d8880d6304cc"),
                new DimensionChunkKey(ResourceLocation.withDefaultNamespace("overworld"), chunk));
        EvolutionEnvironment environment = new FixedEnvironment();

        var first = CandidateSampler.sample(task, 1234L, environment);
        var second = CandidateSampler.sample(task, 1234L, environment);

        assertEquals(first, second);
        assertTrue(first.getX() >= chunk.getMinBlockX() && first.getX() <= chunk.getMaxBlockX());
        assertTrue(first.getZ() >= chunk.getMinBlockZ() && first.getZ() <= chunk.getMaxBlockZ());
        assertEquals(77, first.getY());
    }

    private static final class FixedEnvironment implements EvolutionEnvironment {
        @Override
        public boolean riftExists(UUID riftId) {
            return true;
        }

        @Override
        public boolean chunkLoaded(DimensionChunkKey chunk) {
            return true;
        }

        @Override
        public int coherence(DimensionChunkKey chunk) {
            return 50;
        }

        @Override
        public int surfaceY(DimensionChunkKey chunk, int blockX, int blockZ) {
            return 77;
        }
    }
}
