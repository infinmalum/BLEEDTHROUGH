package com.bleedthrough.meatscape.world.data;

import static org.junit.jupiter.api.Assertions.*;
import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import com.bleedthrough.meatscape.core.migration.DataSchema;
import java.util.Collections;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class ThermalStateTest {
    private static final ResourceLocation ICE = ResourceLocation.withDefaultNamespace("ice_spikes");
    private static final DimensionChunkKey KEY = new DimensionChunkKey(
            ResourceLocation.withDefaultNamespace("overworld"), new ChunkPos(-8, 20));

    @Test void capChangesGraduallyAcrossMixedChunksAndIsBounded() {
        assertEquals(100, ThermalProfile.coherenceCap(0));
        assertEquals(58, ThermalProfile.coherenceCap(8));
        assertEquals(15, ThermalProfile.coherenceCap(16));
        assertEquals(15, ThermalProfile.coherenceCap(Integer.MAX_VALUE));
        assertEquals(100, ThermalProfile.coherenceCap(Integer.MIN_VALUE));
        for (int i = 1; i <= 16; i++) assertTrue(ThermalProfile.coherenceCap(i) < ThermalProfile.coherenceCap(i - 1));
    }

    @Test void versionSixMigrationDoesNotInventSuppressionOrClimateHistory() {
        var tag = new CompoundTag();
        tag.putInt("SchemaVersion", 6);
        var data = MeatscapeWorldData.load(tag);
        assertEquals(DataSchema.WORLD_CURRENT, data.schemaVersion());
        assertTrue(data.thermalProfile(KEY).isEmpty());
        assertEquals(0, data.suppressionTicks(KEY));
    }

    @Test void biomeIdentitiesSurviveReloadButTagMembershipIsNotBakedIntoSave() {
        var profile = new ThermalProfile(Collections.nCopies(16, ICE));
        var data = new MeatscapeWorldData();
        data.rememberThermalProfile(KEY, profile);
        data = MeatscapeWorldData.load(data.save(new CompoundTag()));
        var restored = data.thermalProfile(KEY).orElseThrow();
        assertEquals(profile, restored);
        assertEquals(16, restored.coldSamples(ICE::equals));
        assertEquals(0, restored.coldSamples(id -> false));
        assertNull(ThermalProfile.load(new ListTag()));
        assertThrows(IllegalArgumentException.class, () -> new ThermalProfile(Collections.nCopies(17, ICE)));
    }

    @Test void suppressionPersistsPausesExpiresAndRefreshesWithoutStacking() {
        var data = new MeatscapeWorldData();
        data.addPendingCoherence(KEY, 80);
        data.suppress(KEY);
        assertEquals(0, data.pendingCoherence(KEY));
        for (int i = 0; i < 400; i++) data.tickSuppression();
        data = MeatscapeWorldData.load(data.save(new CompoundTag()));
        assertEquals(800, data.suppressionTicks(KEY));
        data.setPaused(true);
        data.tickSuppression();
        assertEquals(800, data.suppressionTicks(KEY));
        data.setPaused(false);
        data.suppress(KEY);
        assertEquals(1200, data.suppressionTicks(KEY));
        for (int i = 0; i < 1201; i++) data.tickSuppression();
        assertEquals(0, data.suppressionTicks(KEY));
        assertTrue(data.save(new CompoundTag()).getList("Suppression", 10).isEmpty());
    }

    @Test void unloadedDeltasAreCappedWithoutNegativeOrOverflowingValues() {
        var data = new MeatscapeWorldData();
        for (int i = 0; i < 2000; i++) {
            data.addPendingCoherence(KEY, 100);
            data.capPendingCoherence(KEY, 15);
        }
        assertEquals(15, data.pendingCoherence(KEY));
        data = MeatscapeWorldData.load(data.save(new CompoundTag()));
        assertEquals(15, data.consumePendingCoherence(KEY));
        data.addPendingCoherence(KEY, 10);
        data.capPendingCoherence(KEY, -10);
        assertEquals(0, data.pendingChunkCount());
    }
}
