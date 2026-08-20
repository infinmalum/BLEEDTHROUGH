package com.bleedthrough.meatscape.coherence.rollback;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RollbackJobTest {
    @Test void cursorVisitsBoundedRegionExactlyOnceAndRoundTrips() {
        RollbackJob job = new RollbackJob(UUID.randomUUID(), ResourceLocation.withDefaultNamespace("overworld"),
                new BlockPos(1, 4, 2), new BlockPos(2, 5, 3), 3, false);
        assertEquals(8, job.volume());
        assertEquals(new BlockPos(1, 4, 2), job.currentPosition());
        job.advance(true);
        job.advance(false);
        job.advance(false);
        RollbackJob restored = RollbackJob.load(job.save());
        assertEquals(3, restored.cursor());
        assertEquals(1, restored.restored());
        assertEquals(2, restored.skipped());
        assertEquals(job.currentPosition(), restored.currentPosition());
        while (!restored.complete()) restored.advance(false);
        assertEquals(8, restored.cursor());
        assertTrue(restored.complete());
    }

    @Test void normalizesBoundsAndInvalidRate() {
        RollbackJob job = new RollbackJob(UUID.randomUUID(), ResourceLocation.withDefaultNamespace("overworld"),
                new BlockPos(4, 8, 12), new BlockPos(2, 6, 10), 0, true);
        assertEquals(new BlockPos(2, 6, 10), job.min());
        assertEquals(new BlockPos(4, 8, 12), job.max());
        assertEquals(1, job.rate());
        assertTrue(job.dryRun());
    }
}
