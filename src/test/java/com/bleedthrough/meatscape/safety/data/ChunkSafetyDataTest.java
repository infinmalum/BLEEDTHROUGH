package com.bleedthrough.meatscape.safety.data;

import static org.junit.jupiter.api.Assertions.*;

import com.bleedthrough.meatscape.safety.TerrainTrust;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class ChunkSafetyDataTest {
    @Test void defaultsToLegacySafeUnknownAndTracksSparsePositions() {
        AtomicInteger dirty = new AtomicInteger();
        ChunkSafetyData data = new ChunkSafetyData(dirty::incrementAndGet);
        BlockPos low = new BlockPos(1, -2, 15);
        BlockPos high = new BlockPos(15, 80, 1);
        assertEquals(TerrainTrust.UNKNOWN, data.trust());
        data.markModified(low);
        data.markModified(high);
        data.markModified(low);
        assertEquals(2, data.modifiedCount());
        assertEquals(2, dirty.get());
        assertTrue(data.isModified(low));
        data.clearModified(low);
        assertFalse(data.isModified(low));
        assertEquals(1, data.modifiedCount());
    }

    @Test void roundTripsCompactBitmapAndTrust() {
        ChunkSafetyData source = new ChunkSafetyData(() -> { });
        source.setTrust(TerrainTrust.PLAYER_MODIFIED);
        source.markModified(new BlockPos(2, 64, 3));
        CompoundTag encoded = source.serialize();
        assertTrue(encoded.toString().length() < 300, "single provenance bit should remain compact");
        ChunkSafetyData restored = new ChunkSafetyData(() -> { });
        restored.deserialize(encoded);
        assertEquals(TerrainTrust.PLAYER_MODIFIED, restored.trust());
        assertTrue(restored.isModified(new BlockPos(2, 64, 3)));
    }

    @Test void missingSchemaRemainsUnknownForOldChunks() {
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("TerrainTrust", TerrainTrust.TRUSTED.ordinal());
        ChunkSafetyData data = new ChunkSafetyData(() -> { });
        data.deserialize(legacy);
        assertEquals(TerrainTrust.UNKNOWN, data.trust());
    }
}
