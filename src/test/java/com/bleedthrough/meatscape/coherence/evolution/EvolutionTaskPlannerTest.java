package com.bleedthrough.meatscape.coherence.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import com.bleedthrough.meatscape.coherence.rift.RiftRecord;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class EvolutionTaskPlannerTest {
    @Test
    void restartRebuildIncludesOnlyLoadedChunksWithinTheRiftField() {
        ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");
        RiftRecord persistedRift = new RiftRecord(
                UUID.randomUUID(), dimension, new BlockPos(8, 64, 8), 32, 60, 0L, RiftRecord.PERMANENT);
        Set<DimensionChunkKey> loadedAfterRestart = Set.of(
                new DimensionChunkKey(dimension, ChunkPos.ZERO),
                new DimensionChunkKey(dimension, new ChunkPos(1, 0)),
                new DimensionChunkKey(dimension, new ChunkPos(30, 30)));

        var rebuilt = EvolutionTaskPlanner.forRift(persistedRift, loadedAfterRestart::contains);

        assertEquals(2, rebuilt.size());
        assertTrue(rebuilt.stream().allMatch(task -> loadedAfterRestart.contains(task.chunk())));
        assertTrue(rebuilt.stream().noneMatch(task -> task.chunk().pos().equals(new ChunkPos(30, 30))));
    }
}
