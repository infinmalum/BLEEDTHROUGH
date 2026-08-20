package com.bleedthrough.meatscape.coherence.rollback;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Persistent bounded region scan. Cursor persistence makes restart deterministic without storing a position queue. */
public final class RollbackJob {
    private final UUID id;
    private final ResourceLocation dimension;
    private final BlockPos min;
    private final BlockPos max;
    private final int rate;
    private final boolean dryRun;
    private long cursor;
    private long restored;
    private long skipped;

    public RollbackJob(UUID id, ResourceLocation dimension, BlockPos min, BlockPos max, int rate, boolean dryRun) {
        this(id, dimension, min, max, rate, dryRun, 0, 0, 0);
    }

    private RollbackJob(UUID id, ResourceLocation dimension, BlockPos a, BlockPos b, int rate,
            boolean dryRun, long cursor, long restored, long skipped) {
        this.id = id;
        this.dimension = dimension;
        this.min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        this.max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        this.rate = Math.max(1, rate);
        this.dryRun = dryRun;
        this.cursor = Math.max(0, Math.min(cursor, volume()));
        this.restored = Math.max(0, restored);
        this.skipped = Math.max(0, skipped);
    }

    public UUID id() { return id; }
    public ResourceLocation dimension() { return dimension; }
    public BlockPos min() { return min; }
    public BlockPos max() { return max; }
    public int rate() { return rate; }
    public boolean dryRun() { return dryRun; }
    public long cursor() { return cursor; }
    public long restored() { return restored; }
    public long skipped() { return skipped; }
    public long volume() {
        return (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
    }
    public boolean complete() { return cursor >= volume(); }

    public BlockPos currentPosition() {
        if (complete()) return max;
        int sizeX = max.getX() - min.getX() + 1;
        int sizeZ = max.getZ() - min.getZ() + 1;
        long layer = (long) sizeX * sizeZ;
        int y = (int) (cursor / layer);
        long inLayer = cursor % layer;
        int z = (int) (inLayer / sizeX);
        int x = (int) (inLayer % sizeX);
        return min.offset(x, y, z);
    }

    public void advance(boolean restorable) {
        if (!complete()) cursor++;
        if (restorable) restored++; else skipped++;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Min", min.asLong());
        tag.putLong("Max", max.asLong());
        tag.putInt("Rate", rate);
        tag.putBoolean("DryRun", dryRun);
        tag.putLong("Cursor", cursor);
        tag.putLong("Restored", restored);
        tag.putLong("Skipped", skipped);
        return tag;
    }

    public static RollbackJob load(CompoundTag tag) {
        return new RollbackJob(tag.getUUID("Id"), ResourceLocation.parse(tag.getString("Dimension")),
                BlockPos.of(tag.getLong("Min")), BlockPos.of(tag.getLong("Max")), tag.getInt("Rate"),
                tag.getBoolean("DryRun"), tag.getLong("Cursor"), tag.getLong("Restored"), tag.getLong("Skipped"));
    }
}
