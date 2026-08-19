package com.bleedthrough.meatscape.world.data;

import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import com.bleedthrough.meatscape.coherence.rift.RiftRecord;
import com.bleedthrough.meatscape.coherence.rift.RiftSpatialIndex;
import com.bleedthrough.meatscape.coherence.data.MawCoherenceData;
import com.bleedthrough.meatscape.core.migration.DataSchema;
import com.bleedthrough.meatscape.safety.ProtectedRegion;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-wide persistent state owned by the authoritative server. */
public final class MeatscapeWorldData extends SavedData {
    public static final String DATA_NAME = "meatscape_world";
    static final String SCHEMA_KEY = "SchemaVersion";
    static final String STAGE_KEY = "WorldStage";
    static final String PAUSED_KEY = "Paused";
    static final String RIFTS_KEY = "Rifts";
    static final String PENDING_KEY = "PendingCoherence";
    static final String VALUE_KEY = "Value";
    static final String PROTECTED_REGIONS_KEY = "ProtectedRegions";

    private final int schemaVersion;
    private WorldStage worldStage;
    private boolean paused;
    private final Map<UUID, RiftRecord> rifts;
    private final Map<DimensionChunkKey, Integer> pendingCoherence;
    private final RiftSpatialIndex spatialIndex;
    private final Map<UUID, ProtectedRegion> protectedRegions;

    public MeatscapeWorldData() {
        this(DataSchema.WORLD_CURRENT, WorldStage.DORMANT, false, Map.of(), Map.of(), Map.of());
        setDirty();
    }

    private MeatscapeWorldData(
            int schemaVersion,
            WorldStage worldStage,
            boolean paused,
            Map<UUID, RiftRecord> rifts,
            Map<DimensionChunkKey, Integer> pendingCoherence,
            Map<UUID, ProtectedRegion> protectedRegions) {
        this.schemaVersion = schemaVersion;
        this.worldStage = worldStage;
        this.paused = paused;
        this.rifts = new LinkedHashMap<>(rifts);
        this.pendingCoherence = new LinkedHashMap<>(pendingCoherence);
        this.spatialIndex = new RiftSpatialIndex();
        this.spatialIndex.rebuild(this.rifts.values());
        this.protectedRegions = new LinkedHashMap<>(protectedRegions);
    }

    public static MeatscapeWorldData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(MeatscapeWorldData::load, MeatscapeWorldData::new, DATA_NAME);
    }

    public static MeatscapeWorldData load(CompoundTag tag) {
        WorldStage stage = tag.contains(STAGE_KEY, Tag.TAG_ANY_NUMERIC)
                ? WorldStage.fromId(tag.getInt(STAGE_KEY))
                : WorldStage.DORMANT;
        Map<UUID, RiftRecord> rifts = new LinkedHashMap<>();
        ListTag riftTags = tag.getList(RIFTS_KEY, Tag.TAG_COMPOUND);
        for (Tag riftTag : riftTags) {
            RiftRecord rift = RiftRecord.load((CompoundTag) riftTag);
            rifts.put(rift.id(), rift);
        }
        Map<DimensionChunkKey, Integer> pending = new LinkedHashMap<>();
        ListTag pendingTags = tag.getList(PENDING_KEY, Tag.TAG_COMPOUND);
        for (Tag pendingTag : pendingTags) {
            CompoundTag entry = (CompoundTag) pendingTag;
            int value = clamp(entry.getInt(VALUE_KEY));
            if (value > 0) {
                pending.put(DimensionChunkKey.load(entry), value);
            }
        }
        Map<UUID, ProtectedRegion> regions = new LinkedHashMap<>();
        for (Tag regionTag : tag.getList(PROTECTED_REGIONS_KEY, Tag.TAG_COMPOUND)) {
            ProtectedRegion region = ProtectedRegion.load((CompoundTag) regionTag);
            regions.put(region.id(), region);
        }
        return new MeatscapeWorldData(
                DataSchema.WORLD_CURRENT, stage, tag.getBoolean(PAUSED_KEY), rifts, pending, regions);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(SCHEMA_KEY, DataSchema.WORLD_CURRENT);
        tag.putInt(STAGE_KEY, worldStage.id());
        tag.putBoolean(PAUSED_KEY, paused);
        ListTag riftTags = new ListTag();
        rifts.values().stream().map(RiftRecord::save).forEach(riftTags::add);
        tag.put(RIFTS_KEY, riftTags);
        ListTag pendingTags = new ListTag();
        pendingCoherence.forEach((key, value) -> {
            CompoundTag entry = key.save();
            entry.putInt(VALUE_KEY, value);
            pendingTags.add(entry);
        });
        tag.put(PENDING_KEY, pendingTags);
        ListTag regionTags = new ListTag();
        protectedRegions.values().stream().map(ProtectedRegion::save).forEach(regionTags::add);
        tag.put(PROTECTED_REGIONS_KEY, regionTags);
        return tag;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public WorldStage worldStage() {
        return worldStage;
    }

    public void setWorldStage(WorldStage worldStage) {
        if (this.worldStage != worldStage) {
            this.worldStage = worldStage;
            setDirty();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        if (this.paused != paused) {
            this.paused = paused;
            setDirty();
        }
    }

    public Collection<RiftRecord> rifts() {
        return List.copyOf(rifts.values());
    }

    public Optional<RiftRecord> findRift(UUID id) {
        return Optional.ofNullable(rifts.get(id));
    }

    public void addRift(RiftRecord rift) {
        rifts.put(rift.id(), rift);
        spatialIndex.add(rift);
        setDirty();
    }

    public boolean removeRift(UUID id) {
        if (rifts.remove(id) == null) {
            return false;
        }
        spatialIndex.remove(id);
        setDirty();
        return true;
    }

    public RiftSpatialIndex spatialIndex() {
        return spatialIndex;
    }

    public int pendingCoherence(DimensionChunkKey key) {
        return pendingCoherence.getOrDefault(key, 0);
    }

    public void addPendingCoherence(DimensionChunkKey key, int delta) {
        if (delta <= 0) {
            return;
        }
        int previous = pendingCoherence.getOrDefault(key, 0);
        int updated = clamp(previous + delta);
        if (updated != previous) {
            pendingCoherence.put(key, updated);
            setDirty();
        }
    }

    public int consumePendingCoherence(DimensionChunkKey key) {
        Integer value = pendingCoherence.remove(key);
        if (value == null) {
            return 0;
        }
        setDirty();
        return value;
    }

    public int pendingChunkCount() {
        return pendingCoherence.size();
    }

    public Collection<ProtectedRegion> protectedRegions() {
        return List.copyOf(protectedRegions.values());
    }

    public void addProtectedRegion(ProtectedRegion region) {
        protectedRegions.put(region.id(), region);
        setDirty();
    }

    public boolean removeProtectedRegion(UUID id) {
        if (protectedRegions.remove(id) == null) return false;
        setDirty();
        return true;
    }

    public boolean removeAnchorRegion(net.minecraft.resources.ResourceLocation dimension, net.minecraft.core.BlockPos anchor) {
        boolean removed = protectedRegions.values().removeIf(region -> dimension.equals(region.dimension()) && anchor.equals(region.anchor()));
        if (removed) setDirty();
        return removed;
    }

    public boolean isProtected(net.minecraft.resources.ResourceLocation dimension, net.minecraft.core.BlockPos pos) {
        return protectedRegions.values().stream().anyMatch(region -> region.contains(dimension, pos));
    }

    private static int clamp(int value) {
        return MawCoherenceData.clamp(value);
    }
}
