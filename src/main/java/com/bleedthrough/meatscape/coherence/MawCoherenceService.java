package com.bleedthrough.meatscape.coherence;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.data.MawCoherenceCapability;
import com.bleedthrough.meatscape.coherence.data.MawCoherenceData;
import com.bleedthrough.meatscape.core.network.CoherenceSyncPayload;
import com.bleedthrough.meatscape.core.network.MeatscapeNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

/** Authoritative server API for querying and mutating Maw Coherence. */
public final class MawCoherenceService {
    private MawCoherenceService() {
    }

    public static int get(LevelChunk chunk) {
        return data(chunk).value();
    }

    public static int get(ServerLevel level, ChunkPos chunkPos) {
        return get(level.getChunk(chunkPos.x, chunkPos.z));
    }

    public static int set(ServerLevel level, ChunkPos chunkPos, int requestedValue) {
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        MawCoherenceData data = data(chunk);
        data.setValue(requestedValue);
        CoherenceSyncPayload payload = payload(level, chunkPos, data.value());
        MeatscapeNetwork.sendToTracking(chunk, payload);
        Meatscape.LOGGER.info("Set Maw Coherence dimension={} chunk={} value={}",
                level.dimension().location(), chunkPos, data.value());
        return data.value();
    }

    public static CoherenceSyncPayload payload(ServerLevel level, ChunkPos chunkPos, int value) {
        return new CoherenceSyncPayload(level.dimension().location(), chunkPos, value);
    }

    private static MawCoherenceData data(LevelChunk chunk) {
        return chunk.getCapability(MawCoherenceCapability.INSTANCE)
                .orElseThrow(() -> new IllegalStateException("Missing Maw Coherence capability for " + chunk.getPos()));
    }
}
