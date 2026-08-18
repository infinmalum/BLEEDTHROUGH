package com.bleedthrough.meatscape.coherence.rift;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Persistent, entity-free description of an abstract Rift source. */
public record RiftRecord(
        UUID id,
        ResourceLocation dimension,
        BlockPos position,
        int radius,
        int strength,
        long createdGameTime,
        long lifetimeTicks) {
    public static final int MIN_RADIUS = 1;
    public static final int MAX_RADIUS = 4_096;
    public static final int MIN_STRENGTH = 1;
    public static final int MAX_STRENGTH = 100;
    public static final long PERMANENT = -1L;

    private static final String ID_KEY = "Id";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String POSITION_KEY = "Position";
    private static final String RADIUS_KEY = "Radius";
    private static final String STRENGTH_KEY = "Strength";
    private static final String CREATED_KEY = "CreatedGameTime";
    private static final String LIFETIME_KEY = "LifetimeTicks";

    public RiftRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
        radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
        strength = Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, strength));
        createdGameTime = Math.max(0L, createdGameTime);
        lifetimeTicks = lifetimeTicks == PERMANENT ? PERMANENT : Math.max(1L, lifetimeTicks);
    }

    public boolean isExpired(long gameTime) {
        return lifetimeTicks != PERMANENT && gameTime - createdGameTime >= lifetimeTicks;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(ID_KEY, id);
        tag.putString(DIMENSION_KEY, dimension.toString());
        tag.putLong(POSITION_KEY, position.asLong());
        tag.putInt(RADIUS_KEY, radius);
        tag.putInt(STRENGTH_KEY, strength);
        tag.putLong(CREATED_KEY, createdGameTime);
        tag.putLong(LIFETIME_KEY, lifetimeTicks);
        return tag;
    }

    public static RiftRecord load(CompoundTag tag) {
        UUID id = tag.hasUUID(ID_KEY) ? tag.getUUID(ID_KEY) : UUID.randomUUID();
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(DIMENSION_KEY));
        if (dimension == null) {
            dimension = ResourceLocation.withDefaultNamespace("overworld");
        }
        BlockPos position = tag.contains(POSITION_KEY, Tag.TAG_ANY_NUMERIC)
                ? BlockPos.of(tag.getLong(POSITION_KEY))
                : BlockPos.ZERO;
        return new RiftRecord(
                id,
                dimension,
                position,
                tag.getInt(RADIUS_KEY),
                tag.getInt(STRENGTH_KEY),
                tag.getLong(CREATED_KEY),
                tag.contains(LIFETIME_KEY, Tag.TAG_ANY_NUMERIC) ? tag.getLong(LIFETIME_KEY) : PERMANENT);
    }
}
