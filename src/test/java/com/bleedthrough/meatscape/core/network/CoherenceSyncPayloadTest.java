package com.bleedthrough.meatscape.core.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class CoherenceSyncPayloadTest {
    @Test
    void packetRoundTripPreservesDimensionChunkAndValue() {
        CoherenceSyncPayload expected = new CoherenceSyncPayload(id("the_nether"), new ChunkPos(-12, 34), 61);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CoherenceSyncPayload.encode(expected, buffer);
            assertEquals(expected, CoherenceSyncPayload.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void identicalPayloadProducesConsistentStateForMultipleWatchers() {
        CoherenceSyncPayload authoritative = new CoherenceSyncPayload(id("overworld"), new ChunkPos(4, 9), 73);

        CoherenceSyncPayload firstWatcher = roundTrip(authoritative);
        CoherenceSyncPayload secondWatcher = roundTrip(authoritative);

        assertEquals(firstWatcher, secondWatcher);
        assertEquals(73, firstWatcher.coherence());
    }

    @Test
    void receivedValuesAreDefensivelyClamped() {
        assertEquals(100, new CoherenceSyncPayload(id("the_end"), ChunkPos.ZERO, 800).coherence());
        assertEquals(0, new CoherenceSyncPayload(id("the_end"), ChunkPos.ZERO, -1).coherence());
    }

    private static CoherenceSyncPayload roundTrip(CoherenceSyncPayload payload) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CoherenceSyncPayload.encode(payload, buffer);
            return CoherenceSyncPayload.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
