package com.bleedthrough.meatscape.safety;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Persistent axis-aligned settlement protection, optionally owned by a Base Anchor. */
public record ProtectedRegion(UUID id, ResourceLocation dimension, BlockPos min, BlockPos max, BlockPos anchor) {
    public ProtectedRegion {
        BlockPos low = new BlockPos(Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()));
        BlockPos high = new BlockPos(Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ()));
        min = low;
        max = high;
    }

    public boolean contains(ResourceLocation dimension, BlockPos pos) {
        return this.dimension.equals(dimension) && pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Min", min.asLong());
        tag.putLong("Max", max.asLong());
        if (anchor != null) tag.putLong("Anchor", anchor.asLong());
        return tag;
    }

    public static ProtectedRegion load(CompoundTag tag) {
        return new ProtectedRegion(tag.getUUID("Id"), ResourceLocation.parse(tag.getString("Dimension")),
                BlockPos.of(tag.getLong("Min")), BlockPos.of(tag.getLong("Max")),
                tag.contains("Anchor") ? BlockPos.of(tag.getLong("Anchor")) : null);
    }
}
