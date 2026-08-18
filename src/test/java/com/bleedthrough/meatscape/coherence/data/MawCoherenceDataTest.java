package com.bleedthrough.meatscape.coherence.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class MawCoherenceDataTest {
    @Test
    void newChunksStartAtZeroWithoutBeingDirty() {
        AtomicInteger dirtyCalls = new AtomicInteger();
        MawCoherenceData data = new MawCoherenceData(dirtyCalls::incrementAndGet);

        assertEquals(0, data.value());
        assertEquals(0, dirtyCalls.get());
    }

    @Test
    void mutationsClampAndOnlyMarkChangedValuesDirty() {
        AtomicInteger dirtyCalls = new AtomicInteger();
        MawCoherenceData data = new MawCoherenceData(dirtyCalls::incrementAndGet);

        assertFalse(data.setValue(-50));
        assertTrue(data.setValue(150));
        assertFalse(data.setValue(100));
        assertEquals(100, data.value());
        assertEquals(1, dirtyCalls.get());
    }

    @Test
    void serializationRoundTripPreservesValueWithoutDirtyingOnLoad() {
        MawCoherenceData source = new MawCoherenceData(() -> { });
        source.setValue(47);
        AtomicInteger dirtyCalls = new AtomicInteger();
        MawCoherenceData restored = new MawCoherenceData(dirtyCalls::incrementAndGet);

        restored.deserialize(source.serialize());

        assertEquals(47, restored.value());
        assertEquals(0, dirtyCalls.get());
    }

    @Test
    void invalidCurrentDataRecoversToSafeDefaultsOrBounds() {
        MawCoherenceData data = new MawCoherenceData(() -> { });
        CompoundTag wrongType = new CompoundTag();
        wrongType.putInt(MawCoherenceData.SCHEMA_KEY, 1);
        wrongType.putString(MawCoherenceData.VALUE_KEY, "invalid");
        data.deserialize(wrongType);
        assertEquals(0, data.value());

        CompoundTag tooHigh = new CompoundTag();
        tooHigh.putInt(MawCoherenceData.SCHEMA_KEY, 1);
        tooHigh.putInt(MawCoherenceData.VALUE_KEY, 1_000);
        data.deserialize(tooHigh);
        assertEquals(100, data.value());
    }

    @Test
    void versionZeroFractionMigratesRepeatablyToVersionOnePercent() {
        CompoundTag fixture = new CompoundTag();
        fixture.putDouble(MawCoherenceData.LEGACY_VALUE_KEY, 0.375);

        MawCoherenceData first = new MawCoherenceData(() -> { });
        MawCoherenceData second = new MawCoherenceData(() -> { });
        first.deserialize(fixture.copy());
        second.deserialize(fixture.copy());

        assertEquals(38, first.value());
        assertEquals(first.serialize(), second.serialize());
        assertEquals(1, first.serialize().getInt(MawCoherenceData.SCHEMA_KEY));
    }

    @Test
    void nonFiniteLegacyValuesRecoverToDefault() {
        CompoundTag fixture = new CompoundTag();
        fixture.putDouble(MawCoherenceData.LEGACY_VALUE_KEY, Double.NaN);
        MawCoherenceData data = new MawCoherenceData(() -> { });

        data.deserialize(fixture);

        assertEquals(0, data.value());
    }
}
