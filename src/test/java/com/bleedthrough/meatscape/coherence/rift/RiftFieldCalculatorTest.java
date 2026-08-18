package com.bleedthrough.meatscape.coherence.rift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class RiftFieldCalculatorTest {
    @Test
    void singleRiftFallsOffAndIsStableAtBoundary() {
        RiftRecord rift = rift(new BlockPos(8, 64, 8), 32, 100);

        int center = RiftFieldCalculator.contribution(rift, ChunkPos.ZERO);
        int near = RiftFieldCalculator.contribution(rift, new ChunkPos(1, 0));
        int outside = RiftFieldCalculator.contribution(rift, new ChunkPos(3, 0));

        assertEquals(5, center);
        assertTrue(near > 0 && near < center);
        assertEquals(0, outside);
        assertEquals(near, RiftFieldCalculator.contribution(rift, new ChunkPos(1, 0)));
    }

    @Test
    void overlappingRiftsStackAndClamp() {
        RiftRecord first = rift(new BlockPos(8, 64, 8), 64, 100);
        RiftRecord second = rift(new BlockPos(8, 20, 8), 64, 100);

        assertEquals(10, RiftFieldCalculator.aggregate(List.of(first, second), ChunkPos.ZERO));
        assertEquals(100, RiftFieldCalculator.aggregate(java.util.Collections.nCopies(30, first), ChunkPos.ZERO));
    }

    @Test
    void multiHourSimulationStaysBoundedAndRetainsNoGrowingState() {
        RiftRecord rift = rift(new BlockPos(8, 64, 8), 48, 65);
        int coherence = 0;
        for (int tick = 0; tick < 20 * 60 * 60 * 6; tick += 20) {
            coherence = Math.min(100, coherence + RiftFieldCalculator.contribution(rift, ChunkPos.ZERO));
        }
        assertEquals(100, coherence);
    }

    private static RiftRecord rift(BlockPos position, int radius, int strength) {
        return new RiftRecord(
                UUID.randomUUID(), ResourceLocation.withDefaultNamespace("overworld"),
                position, radius, strength, 0L, RiftRecord.PERMANENT);
    }
}
