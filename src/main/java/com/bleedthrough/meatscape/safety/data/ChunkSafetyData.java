package com.bleedthrough.meatscape.safety.data;

import com.bleedthrough.meatscape.safety.TerrainTrust;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Compact per-chunk provenance: one bit only for candidate positions known unsafe. */
public final class ChunkSafetyData {
    public static final int SCHEMA_VERSION = 1;
    static final String SCHEMA_KEY = "SchemaVersion";
    static final String TRUST_KEY = "TerrainTrust";
    static final String SECTIONS_KEY = "ModifiedSections";
    static final String SECTION_Y_KEY = "Y";
    static final String BITS_KEY = "Bits";

    private final Runnable dirtyCallback;
    private final Map<Integer, BitSet> modifiedSections = new HashMap<>();
    private TerrainTrust trust = TerrainTrust.UNKNOWN;

    public ChunkSafetyData(Runnable dirtyCallback) {
        this.dirtyCallback = dirtyCallback;
    }

    public TerrainTrust trust() {
        return trust;
    }

    public void setTrust(TerrainTrust trust) {
        if (this.trust != trust) {
            this.trust = trust;
            dirtyCallback.run();
        }
    }

    public boolean isModified(BlockPos pos) {
        BitSet bits = modifiedSections.get(pos.getY() >> 4);
        return bits != null && bits.get(localIndex(pos));
    }

    public void markModified(BlockPos pos) {
        BitSet bits = modifiedSections.computeIfAbsent(pos.getY() >> 4, ignored -> new BitSet(4096));
        int index = localIndex(pos);
        if (!bits.get(index)) {
            bits.set(index);
            dirtyCallback.run();
        }
    }

    public void clearModified(BlockPos pos) {
        int sectionY = pos.getY() >> 4;
        BitSet bits = modifiedSections.get(sectionY);
        if (bits != null && bits.get(localIndex(pos))) {
            bits.clear(localIndex(pos));
            if (bits.isEmpty()) modifiedSections.remove(sectionY);
            dirtyCallback.run();
        }
    }

    public int modifiedCount() {
        return modifiedSections.values().stream().mapToInt(BitSet::cardinality).sum();
    }

    public CompoundTag serialize() {
        CompoundTag root = new CompoundTag();
        root.putInt(SCHEMA_KEY, SCHEMA_VERSION);
        root.putInt(TRUST_KEY, trust.ordinal());
        ListTag sections = new ListTag();
        modifiedSections.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag section = new CompoundTag();
            section.putInt(SECTION_Y_KEY, entry.getKey());
            section.putLongArray(BITS_KEY, entry.getValue().toLongArray());
            sections.add(section);
        });
        root.put(SECTIONS_KEY, sections);
        return root;
    }

    public void deserialize(CompoundTag root) {
        modifiedSections.clear();
        trust = root.contains(SCHEMA_KEY, Tag.TAG_ANY_NUMERIC)
                ? TerrainTrust.fromId(root.getInt(TRUST_KEY)) : TerrainTrust.UNKNOWN;
        ListTag sections = root.getList(SECTIONS_KEY, Tag.TAG_COMPOUND);
        for (Tag value : sections) {
            CompoundTag section = (CompoundTag) value;
            BitSet bits = BitSet.valueOf(section.getLongArray(BITS_KEY));
            if (!bits.isEmpty()) modifiedSections.put(section.getInt(SECTION_Y_KEY), bits);
        }
    }

    private static int localIndex(BlockPos pos) {
        return (pos.getY() & 15) << 8 | (pos.getZ() & 15) << 4 | pos.getX() & 15;
    }
}
