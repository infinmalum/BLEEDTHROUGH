package com.bleedthrough.meatscape.world.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bleedthrough.meatscape.core.migration.DataSchema;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class MeatscapeWorldDataTest {
    @Test
    void versionZeroFixtureMigratesToDormantVersionOne() {
        MeatscapeWorldData migrated = MeatscapeWorldData.load(new CompoundTag());

        assertEquals(DataSchema.CURRENT, migrated.schemaVersion());
        assertEquals(WorldStage.DORMANT, migrated.worldStage());
        assertEquals(DataSchema.CURRENT, migrated.save(new CompoundTag()).getInt(MeatscapeWorldData.SCHEMA_KEY));
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
        fixture.putInt(MeatscapeWorldData.SCHEMA_KEY, DataSchema.CURRENT);
        fixture.putInt(MeatscapeWorldData.STAGE_KEY, 99);

        assertEquals(WorldStage.DORMANT, MeatscapeWorldData.load(fixture).worldStage());
    }
}
