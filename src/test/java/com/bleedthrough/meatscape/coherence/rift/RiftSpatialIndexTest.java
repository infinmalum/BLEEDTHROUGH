package com.bleedthrough.meatscape.coherence.rift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class RiftSpatialIndexTest {
    @Test
    void indexFindsOverlappingRiftsAndRemovesWithoutWorldReferences() {
        ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");
        RiftRecord first = rift(dimension, new BlockPos(8, 64, 8), 24);
        RiftRecord second = rift(dimension, new BlockPos(24, 64, 8), 24);
        RiftSpatialIndex index = new RiftSpatialIndex();
        index.rebuild(List.of(first, second));

        assertEquals(2, index.at(dimension, ChunkPos.ZERO).size());
        assertTrue(index.remove(first.id()));
        assertEquals(List.of(second), index.at(dimension, ChunkPos.ZERO));
        assertEquals(1, index.size());
    }

    private static RiftRecord rift(ResourceLocation dimension, BlockPos position, int radius) {
        return new RiftRecord(UUID.randomUUID(), dimension, position, radius, 50, 0L, RiftRecord.PERMANENT);
    }
}
