package com.bleedthrough.meatscape.safety.data;

import static org.junit.jupiter.api.Assertions.*;

import com.bleedthrough.meatscape.safety.TerrainTrust;
import com.bleedthrough.meatscape.coherence.rollback.RestorationSource;
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

    @Test void restorationCategoriesAreSparseRoundTripAndClearable() {
        ChunkSafetyData data = new ChunkSafetyData(() -> { });
        BlockPos stone = new BlockPos(3, 64, 5);
        BlockPos film = new BlockPos(8, -3, 9);
        data.recordRestoration(stone, RestorationSource.STONE);
        data.recordRestoration(film, RestorationSource.ATTACHMENT);
        assertEquals(2, data.restorationCount());
        assertTrue(data.serialize().toString().length() < 500,
                "coarse restoration entries should remain compact");

        ChunkSafetyData restored = new ChunkSafetyData(() -> { });
        restored.deserialize(data.serialize());
        assertEquals(RestorationSource.STONE, restored.restorationSource(stone));
        assertEquals(RestorationSource.ATTACHMENT, restored.restorationSource(film));
        restored.clearRestoration(stone);
        assertNull(restored.restorationSource(stone));
        assertEquals(1, restored.restorationCount());
    }

    @Test void schemaOneMigratesWithoutInventingRestorationHistory() {
        ChunkSafetyData source = new ChunkSafetyData(() -> { });
        source.markModified(new BlockPos(1, 40, 2));
        CompoundTag versionOne = source.serialize();
        versionOne.putInt(ChunkSafetyData.SCHEMA_KEY, 1);
        versionOne.remove(ChunkSafetyData.RESTORATION_KEY);
        ChunkSafetyData restored = new ChunkSafetyData(() -> { });
        restored.deserialize(versionOne);
        assertEquals(1, restored.modifiedCount());
        assertEquals(0, restored.restorationCount());
    }
}
