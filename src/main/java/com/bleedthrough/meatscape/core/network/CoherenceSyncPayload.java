package com.bleedthrough.meatscape.core.network;

import com.bleedthrough.meatscape.coherence.data.MawCoherenceData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

/** Minimal immutable chunk coherence DTO sent from server to client. */
public record CoherenceSyncPayload(ResourceLocation dimension, ChunkPos chunkPos, int coherence) {
    public CoherenceSyncPayload {
        coherence = MawCoherenceData.clamp(coherence);
    }

    public static void encode(CoherenceSyncPayload payload, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(payload.dimension);
        buffer.writeLong(payload.chunkPos.toLong());
        buffer.writeVarInt(payload.coherence);
    }

    public static CoherenceSyncPayload decode(FriendlyByteBuf buffer) {
        return new CoherenceSyncPayload(
                buffer.readResourceLocation(),
                new ChunkPos(buffer.readLong()),
                buffer.readVarInt());
    }
}
