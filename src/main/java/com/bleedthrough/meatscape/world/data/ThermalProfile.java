package com.bleedthrough.meatscape.world.data;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

/** Sixteen quart-biome identities, never holders or references to a loaded chunk. */
public record ThermalProfile(List<ResourceLocation> biomes) {
    public static final int SAMPLE_COUNT = 16;
    public ThermalProfile {
        biomes = List.copyOf(biomes);
        if (biomes.size() != SAMPLE_COUNT) throw new IllegalArgumentException("Expected 16 biome samples");
    }

    public int coldSamples(Predicate<ResourceLocation> cold) {
        return (int) biomes.stream().filter(cold).count();
    }

    public static int coherenceCap(int coldSamples) {
        return 100 - 85 * Math.max(0, Math.min(SAMPLE_COUNT, coldSamples)) / SAMPLE_COUNT;
    }

    public ListTag save() {
        ListTag result = new ListTag();
        biomes.forEach(id -> result.add(StringTag.valueOf(id.toString())));
        return result;
    }

    public static ThermalProfile load(ListTag tag) {
        if (tag.size() != SAMPLE_COUNT) return null;
        var ids = new java.util.ArrayList<ResourceLocation>();
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            var id = ResourceLocation.tryParse(tag.getString(i));
            if (id == null) return null;
            ids.add(id);
        }
        return new ThermalProfile(ids);
    }
}
