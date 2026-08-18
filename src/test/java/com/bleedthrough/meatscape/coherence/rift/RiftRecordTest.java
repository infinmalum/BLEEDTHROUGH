package com.bleedthrough.meatscape.coherence.rift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RiftRecordTest {
    @Test
    void nbtRoundTripPreservesIdentityAndLifecycle() {
        RiftRecord expected = rift(48, 80, 100L, 200L);

        RiftRecord restored = RiftRecord.load(expected.save());

        assertEquals(expected, restored);
        assertFalse(restored.isExpired(299L));
        assertTrue(restored.isExpired(300L));
    }

    @Test
    void constructorClampsUnsafeInputs() {
        RiftRecord value = rift(Integer.MAX_VALUE, Integer.MIN_VALUE, -10L, 0L);

        assertEquals(RiftRecord.MAX_RADIUS, value.radius());
        assertEquals(RiftRecord.MIN_STRENGTH, value.strength());
        assertEquals(0L, value.createdGameTime());
        assertEquals(1L, value.lifetimeTicks());
    }

    private static RiftRecord rift(int radius, int strength, long created, long lifetime) {
        return new RiftRecord(
                UUID.randomUUID(), ResourceLocation.withDefaultNamespace("overworld"),
                new BlockPos(8, 64, 8), radius, strength, created, lifetime);
    }
}
