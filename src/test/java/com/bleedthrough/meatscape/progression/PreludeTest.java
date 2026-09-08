package com.bleedthrough.meatscape.progression;

import static org.junit.jupiter.api.Assertions.*;
import com.bleedthrough.meatscape.client.PreludeClock;
import com.bleedthrough.meatscape.core.network.PreludePayload;
import com.bleedthrough.meatscape.coherence.rift.RiftRecord;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.world.data.WorldStage;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class PreludeTest {
    @Test void versionFivePendingEventStartsBeforePreludeAndReloadResumesWithoutReplaying() {
        var data = new MeatscapeWorldData();
        data.scheduleBleeding(BlockPos.ZERO, 0);
        var rift = new RiftRecord(UUID.randomUUID(), BleedingService.OVERWORLD, new BlockPos(200, 64, 0), 96, 24, 0, -1, false);
        data.addRift(rift);
        var v5 = data.save(new CompoundTag());
        v5.putInt("SchemaVersion", 5);
        v5.remove("PreludeElapsed");
        data = MeatscapeWorldData.load(v5);
        assertEquals(-1, data.preludeElapsed());
        BleedingService.advancePrelude(data, 0);
        assertEquals(0, data.preludeElapsed());
        for (int i = 0; i < 55; i++) assertTrue(BleedingService.advancePrelude(data, i).isEmpty());
        data = MeatscapeWorldData.load(data.save(new CompoundTag()));
        assertEquals(55, data.preludeElapsed());
        data.setPaused(true);
        BleedingService.advancePrelude(data, 56);
        assertEquals(55, data.preludeElapsed());
        data.setPaused(false);
        for (int i = 55; i < 120; i++) assertTrue(BleedingService.advancePrelude(data, i).isEmpty());
        assertEquals(WorldStage.DORMANT, data.worldStage());
        assertEquals(rift.id(), BleedingService.advancePrelude(data, 121).orElseThrow().id());
        assertTrue(BleedingService.advancePrelude(data, 122).isEmpty());
    }

    @Test void missingSourceWaitsAndExplorationRelocatesOnlyBeforePerformance() {
        var data = new MeatscapeWorldData();
        data.scheduleBleeding(BlockPos.ZERO, 0);
        BleedingService.advancePrelude(data, 0);
        assertEquals(-1, data.preludeElapsed());
        var pos = new BlockPos(2000, 64, 0);
        var rift = new RiftRecord(UUID.randomUUID(), BleedingService.OVERWORLD, pos, 96, 24, 0, -1, false);
        data.addRift(rift);
        data.relocateBleeding(pos);
        BleedingService.advancePrelude(data, 1);
        data.relocateBleeding(BlockPos.ZERO);
        assertEquals(pos, data.bleedingOrigin().orElseThrow());
        data.removeRift(rift.id());
        data.setPreludeElapsed(120);
        assertTrue(BleedingService.advancePrelude(data, 122).isEmpty());
        assertEquals(-1, data.preludeElapsed());
        assertEquals(WorldStage.DORMANT, data.worldStage());
    }

    @Test void clientLeaseStopsEffectsAndThumpIsNotReplayedByRepeatedSnapshots() {
        var first = new PreludeClock();
        var second = new PreludeClock();
        assertFalse(first.accept(35));
        assertTrue(first.accept(40));
        assertTrue(second.accept(40));
        assertFalse(first.accept(40));
        assertEquals(first.elapsed(), second.elapsed());
        for (int i = 0; i < 15; i++) first.tick();
        assertEquals(-1, first.elapsed());
        assertFalse(first.accept(40)); // Pause/lease expiry at the thump must not replay it.
        second.clear();
        assertEquals(-1, second.elapsed());
        assertFalse(second.accept(70)); // Late join resumes without replaying the sound.
        assertFalse(second.accept(-1));
        assertEquals(-1, second.elapsed());
    }

    @Test void timelineHasBoundedDarkeningAndQuietRecovery() {
        assertEquals(0, PreludeTimeline.dimming(-1));
        assertEquals(0, PreludeTimeline.dimming(0));
        assertTrue(PreludeTimeline.dimming(20) <= 0.16F);
        assertTrue(PreludeTimeline.anomaly(39));
        assertFalse(PreludeTimeline.anomaly(40));
        assertEquals(0, PreludeTimeline.dimming(60));
        assertFalse(PreludeTimeline.running(120));
    }

    @Test void packetRoundTripsForIndependentObserversAndClampsInvalidValues() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            PreludePayload.encode(new PreludePayload(75), buffer);
            buffer.markReaderIndex();
            var first = PreludePayload.decode(buffer);
            buffer.resetReaderIndex();
            assertEquals(first, PreludePayload.decode(buffer));
            assertEquals(75, first.elapsed());
            assertEquals(120, new PreludePayload(Integer.MAX_VALUE).elapsed());
            assertEquals(-1, new PreludePayload(Integer.MIN_VALUE).elapsed());
        } finally { buffer.release(); }
    }
}
