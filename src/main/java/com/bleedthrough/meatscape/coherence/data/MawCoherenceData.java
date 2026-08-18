package com.bleedthrough.meatscape.coherence.data;

import com.bleedthrough.meatscape.core.migration.DataSchema;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Versioned, chunk-scoped Maw Coherence state. */
public final class MawCoherenceData {
    public static final int MIN = 0;
    public static final int MAX = 100;
    public static final int DEFAULT = MIN;

    static final String SCHEMA_KEY = "SchemaVersion";
    static final String VALUE_KEY = "MawCoherence";
    static final String LEGACY_VALUE_KEY = "coherence";

    private final Runnable dirtyCallback;
    private int value;

    public MawCoherenceData(Runnable dirtyCallback) {
        this.dirtyCallback = dirtyCallback;
        this.value = DEFAULT;
    }

    public int value() {
        return value;
    }

    public boolean setValue(int value) {
        int clamped = clamp(value);
        if (this.value == clamped) {
            return false;
        }
        this.value = clamped;
        dirtyCallback.run();
        return true;
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA_KEY, DataSchema.CURRENT);
        tag.putInt(VALUE_KEY, value);
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        int schema = tag.contains(SCHEMA_KEY, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(SCHEMA_KEY)
                : DataSchema.VERSION_0;
        value = schema <= DataSchema.VERSION_0 ? readLegacy(tag) : readCurrent(tag);
    }

    private static int readCurrent(CompoundTag tag) {
        return tag.contains(VALUE_KEY, Tag.TAG_ANY_NUMERIC) ? clamp(tag.getInt(VALUE_KEY)) : DEFAULT;
    }

    private static int readLegacy(CompoundTag tag) {
        if (!tag.contains(LEGACY_VALUE_KEY, Tag.TAG_ANY_NUMERIC)) {
            return DEFAULT;
        }
        double fraction = tag.getDouble(LEGACY_VALUE_KEY);
        if (!Double.isFinite(fraction)) {
            return DEFAULT;
        }
        return clamp((int) Math.round(fraction * MAX));
    }

    public static int clamp(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }
}
