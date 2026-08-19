package com.bleedthrough.meatscape.world.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bleedthrough.meatscape.core.migration.DataSchema;
import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import com.bleedthrough.meatscape.coherence.rift.RiftRecord;
import com.bleedthrough.meatscape.safety.ProtectedRegion;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class MeatscapeWorldDataTest {
    @Test
    void versionZeroFixtureMigratesToCurrentDormantSchema() {
        MeatscapeWorldData migrated = MeatscapeWorldData.load(new CompoundTag());

        assertEquals(DataSchema.WORLD_CURRENT, migrated.schemaVersion());
        assertEquals(WorldStage.DORMANT, migrated.worldStage());
        assertEquals(DataSchema.WORLD_CURRENT, migrated.save(new CompoundTag()).getInt(MeatscapeWorldData.SCHEMA_KEY));
    }

    @Test
    void stageRoundTripAndDirtyTrackingAreStable() {
        MeatscapeWorldData data = new MeatscapeWorldData();
        assertTrue(data.isDirty());
        data.setDirty(false);

        data.setWorldStage(WorldStage.DORMANT);
        assertFalse(data.isDirty());

        data.setWorldStage(WorldStage.INCURSION);
        assertTrue(data.isDirty());

        MeatscapeWorldData restored = MeatscapeWorldData.load(data.save(new CompoundTag()));
        assertEquals(WorldStage.INCURSION, restored.worldStage());
    }

    @Test
    void invalidStageFallsBackToDormant() {
        CompoundTag fixture = new CompoundTag();
        fixture.putInt(MeatscapeWorldData.SCHEMA_KEY, DataSchema.WORLD_CURRENT);
        fixture.putInt(MeatscapeWorldData.STAGE_KEY, 99);

        assertEquals(WorldStage.DORMANT, MeatscapeWorldData.load(fixture).worldStage());
    }

    @Test
    void versionOneFixtureAddsEmptyRiftStateWithoutLosingStage() {
        CompoundTag fixture = new CompoundTag();
        fixture.putInt(MeatscapeWorldData.SCHEMA_KEY, DataSchema.VERSION_1);
        fixture.putInt(MeatscapeWorldData.STAGE_KEY, WorldStage.ADAPTATION.id());

        MeatscapeWorldData migrated = MeatscapeWorldData.load(fixture);

        assertEquals(DataSchema.WORLD_CURRENT, migrated.schemaVersion());
        assertEquals(WorldStage.ADAPTATION, migrated.worldStage());
        assertFalse(migrated.isPaused());
        assertTrue(migrated.rifts().isEmpty());
        assertEquals(0, migrated.pendingChunkCount());
    }

    @Test void versionTwoAddsEmptyProtectionAndRegionsRoundTrip() {
        CompoundTag old = new CompoundTag();
        old.putInt(MeatscapeWorldData.SCHEMA_KEY, DataSchema.VERSION_2);
        assertTrue(MeatscapeWorldData.load(old).protectedRegions().isEmpty());

        MeatscapeWorldData data = new MeatscapeWorldData();
        ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");
        BlockPos anchor = new BlockPos(4, 70, 9);
        ProtectedRegion region = new ProtectedRegion(UUID.randomUUID(), dimension,
                anchor.offset(-8, -8, -8), anchor.offset(8, 8, 8), anchor);
        data.addProtectedRegion(region);
        MeatscapeWorldData restored = MeatscapeWorldData.load(data.save(new CompoundTag()));
        assertTrue(restored.isProtected(dimension, anchor.offset(5, 0, 0)));
        assertTrue(restored.removeAnchorRegion(dimension, anchor));
        assertFalse(restored.isProtected(dimension, anchor));
    }

    @Test
    void riftsPauseAndPendingCoherenceRoundTrip() {
        MeatscapeWorldData data = new MeatscapeWorldData();
        ResourceLocation dimension = ResourceLocation.withDefaultNamespace("the_nether");
        RiftRecord rift = new RiftRecord(
                UUID.randomUUID(), dimension, new BlockPos(-30, 70, 45), 96, 72, 1200L, 6000L);
        DimensionChunkKey pendingKey = new DimensionChunkKey(dimension, new ChunkPos(-2, 3));
        data.addRift(rift);
        data.setPaused(true);
        data.addPendingCoherence(pendingKey, 35);

        MeatscapeWorldData restored = MeatscapeWorldData.load(data.save(new CompoundTag()));

        assertEquals(rift, restored.findRift(rift.id()).orElseThrow());
        assertTrue(restored.isPaused());
        assertEquals(35, restored.pendingCoherence(pendingKey));
        assertEquals(35, restored.consumePendingCoherence(pendingKey));
        assertEquals(0, restored.pendingChunkCount());
        assertTrue(restored.removeRift(rift.id()));
        assertFalse(restored.removeRift(rift.id()));
    }

    @Test
    void unloadedChunkAccumulatorSaturatesWithoutGrowingPerTickState() {
        MeatscapeWorldData data = new MeatscapeWorldData();
        DimensionChunkKey key = new DimensionChunkKey(
                ResourceLocation.withDefaultNamespace("overworld"), new ChunkPos(400, -300));

        for (int update = 0; update < 60 * 60 * 6; update++) {
            data.addPendingCoherence(key, 3);
        }

        assertEquals(1, data.pendingChunkCount());
        assertEquals(100, data.pendingCoherence(key));
    }
}
