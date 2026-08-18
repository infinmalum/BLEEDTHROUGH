package com.bleedthrough.meatscape.coherence.rift;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

/** Rebuildable chunk index storing only immutable Rift records and scalar keys. */
public final class RiftSpatialIndex {
    private final Map<UUID, RiftRecord> records = new LinkedHashMap<>();
    private final Map<DimensionChunkKey, LinkedHashSet<UUID>> byChunk = new HashMap<>();

    public void rebuild(Collection<RiftRecord> rifts) {
        records.clear();
        byChunk.clear();
        rifts.forEach(this::add);
    }

    public void add(RiftRecord rift) {
        remove(rift.id());
        records.put(rift.id(), rift);
        int minX = Math.floorDiv(rift.position().getX() - rift.radius(), 16);
        int maxX = Math.floorDiv(rift.position().getX() + rift.radius(), 16);
        int minZ = Math.floorDiv(rift.position().getZ() - rift.radius(), 16);
        int maxZ = Math.floorDiv(rift.position().getZ() + rift.radius(), 16);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                byChunk.computeIfAbsent(
                        new DimensionChunkKey(rift.dimension(), new ChunkPos(x, z)),
                        ignored -> new LinkedHashSet<>()).add(rift.id());
            }
        }
    }

    public boolean remove(UUID id) {
        RiftRecord removed = records.remove(id);
        if (removed == null) {
            return false;
        }
        byChunk.values().forEach(ids -> ids.remove(id));
        byChunk.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return true;
    }

    public List<RiftRecord> at(ResourceLocation dimension, ChunkPos chunkPos) {
        var ids = byChunk.get(new DimensionChunkKey(dimension, chunkPos));
        if (ids == null) {
            return List.of();
        }
        List<RiftRecord> result = new ArrayList<>(ids.size());
        ids.stream().map(records::get).filter(java.util.Objects::nonNull).forEach(result::add);
        return List.copyOf(result);
    }

    public int size() {
        return records.size();
    }
}
