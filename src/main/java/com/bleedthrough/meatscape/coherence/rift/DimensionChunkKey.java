package com.bleedthrough.meatscape.coherence.rift;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

/** Stable dimension/chunk key containing no world or chunk references. */
public record DimensionChunkKey(ResourceLocation dimension, long chunkPos) {
    private static final String DIMENSION_KEY = "Dimension";
    private static final String CHUNK_KEY = "Chunk";

    public DimensionChunkKey(ResourceLocation dimension, ChunkPos chunkPos) {
        this(dimension, chunkPos.toLong());
    }

    public ChunkPos pos() {
        return new ChunkPos(chunkPos);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(DIMENSION_KEY, dimension.toString());
        tag.putLong(CHUNK_KEY, chunkPos);
        return tag;
    }

    public static DimensionChunkKey load(CompoundTag tag) {
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(DIMENSION_KEY));
        if (dimension == null) {
            dimension = ResourceLocation.withDefaultNamespace("overworld");
        }
        return new DimensionChunkKey(dimension, tag.getLong(CHUNK_KEY));
    }
}
