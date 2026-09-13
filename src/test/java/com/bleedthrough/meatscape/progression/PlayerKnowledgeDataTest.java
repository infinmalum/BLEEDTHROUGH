package com.bleedthrough.meatscape.progression;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PlayerKnowledgeDataTest {
    @Test void observationsAreIdempotentAndPersistIndependently() {
        AtomicInteger dirty = new AtomicInteger(); PlayerKnowledgeData data = new PlayerKnowledgeData(dirty::incrementAndGet);
        assertTrue(data.observe(KnowledgeObservation.RIFT)); assertFalse(data.observe(KnowledgeObservation.RIFT));
        assertTrue(data.observe(KnowledgeObservation.CAUTERIZATION)); assertEquals(2, dirty.get());
        assertTrue(data.observe(KnowledgeObservation.WORMHOLE)); assertEquals(3, dirty.get());
        PlayerKnowledgeData loaded = new PlayerKnowledgeData(() -> { }); loaded.load(data.save());
        assertTrue(loaded.observed(KnowledgeObservation.RIFT)); assertTrue(loaded.observed(KnowledgeObservation.CAUTERIZATION));
        assertTrue(loaded.observed(KnowledgeObservation.WORMHOLE));
        assertFalse(loaded.observed(KnowledgeObservation.BIOINDUSTRY));
    }
    @Test void unknownAndLegacyDataDoNotInventObservations() {
        PlayerKnowledgeData data = new PlayerKnowledgeData(() -> { });
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.ListTag values = new net.minecraft.nbt.ListTag(); values.add(net.minecraft.nbt.StringTag.valueOf("future")); tag.put("Observed", values);
        data.load(tag); assertTrue(data.observations().isEmpty());
    }
    @Test void cloneCopiesOnlyTheSamePlayersKnowledge() {
        PlayerKnowledgeData first = new PlayerKnowledgeData(() -> { }); first.observe(KnowledgeObservation.RIFT);
        PlayerKnowledgeData respawn = new PlayerKnowledgeData(() -> { }); respawn.copyFrom(first);
        PlayerKnowledgeData secondPlayer = new PlayerKnowledgeData(() -> { });
        assertTrue(respawn.observed(KnowledgeObservation.RIFT)); assertTrue(secondPlayer.observations().isEmpty());
    }
}
