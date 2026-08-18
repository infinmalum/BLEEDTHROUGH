package com.bleedthrough.meatscape.client;

import com.bleedthrough.meatscape.core.network.CoherenceSyncPayload;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

/** Client-side read model containing only coherence values needed for presentation. */
public final class ClientCoherenceState {
    private static final Map<Key, Integer> VALUES = new ConcurrentHashMap<>();

    private ClientCoherenceState() {
    }

    public static void accept(CoherenceSyncPayload payload) {
        VALUES.put(new Key(payload.dimension(), payload.chunkPos().toLong()), payload.coherence());
    }

    public static OptionalInt get(ResourceLocation dimension, ChunkPos chunkPos) {
        Integer value = VALUES.get(new Key(dimension, chunkPos.toLong()));
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    public static void clear() {
        VALUES.clear();
    }

    private record Key(ResourceLocation dimension, long chunkPos) {
    }
}
