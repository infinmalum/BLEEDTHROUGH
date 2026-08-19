package com.bleedthrough.meatscape.coherence.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.time.Duration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class EvolutionSchedulerTest {
    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");

    @Test
    void globalAndPerRiftBudgetsAreNeverExceeded() {
        EvolutionScheduler globalLimited = new EvolutionScheduler(7, 3);
        FakeEnvironment environment = new FakeEnvironment();
        for (int index = 0; index < 30; index++) {
            enqueue(globalLimited, environment, UUID.randomUUID(), index);
        }

        assertEquals(7, globalLimited.tick(false, 100L, environment).size());

        EvolutionScheduler riftLimited = new EvolutionScheduler(100, 3);
        FakeEnvironment secondEnvironment = new FakeEnvironment();
        UUID sharedRift = UUID.randomUUID();
        for (int index = 0; index < 20; index++) {
            enqueue(riftLimited, secondEnvironment, sharedRift, index);
        }
        var candidates = riftLimited.tick(false, 100L, secondEnvironment);
        assertEquals(3, candidates.size());
        assertEquals(17L, riftLimited.stats().skipped().get(EvolutionSkipReason.PER_RIFT_BUDGET));
    }

    @Test
    void unloadDropsTasksAndLeavesOnlyScalarKeys() {
        EvolutionScheduler scheduler = new EvolutionScheduler(8, 8);
        FakeEnvironment environment = new FakeEnvironment();
        UUID rift = UUID.randomUUID();
        DimensionChunkKey first = enqueue(scheduler, environment, rift, 1);
        enqueue(scheduler, environment, rift, 2);

        environment.loaded.remove(first);
        assertEquals(1, scheduler.removeChunk(first));
        assertEquals(1, scheduler.stats().queueLength());
        assertEquals(1, scheduler.tick(false, 2L, environment).size());
        assertTrue(EvolutionTask.class.getRecordComponents()[1].getType().equals(DimensionChunkKey.class));
    }

    @Test
    void pauseResumeAndRebuildAreDeterministic() {
        EvolutionScheduler scheduler = new EvolutionScheduler(4, 4);
        FakeEnvironment environment = new FakeEnvironment();
        UUID rift = UUID.randomUUID();
        DimensionChunkKey key = enqueue(scheduler, environment, rift, 0);

        assertTrue(scheduler.tick(true, 10L, environment).isEmpty());
        assertEquals(1, scheduler.stats().queueLength());
        assertEquals(1, scheduler.tick(false, 11L, environment).size());

        scheduler.clear();
        assertEquals(0, scheduler.stats().queueLength());
        assertTrue(scheduler.enqueue(new EvolutionTask(rift, key)));
        assertEquals(1, scheduler.tick(false, 12L, environment).size());
    }

    @Test
    void rapidChunkLoadEventsAreDeduplicatedWithoutStorms() {
        EvolutionScheduler scheduler = new EvolutionScheduler(16, 16);
        FakeEnvironment environment = new FakeEnvironment();
        UUID rift = UUID.randomUUID();
        DimensionChunkKey key = new DimensionChunkKey(OVERWORLD, ChunkPos.ZERO);
        environment.rifts.add(rift);
        environment.loaded.add(key);
        for (int event = 0; event < 10_000; event++) {
            scheduler.enqueue(new EvolutionTask(rift, key));
        }

        assertEquals(1, scheduler.stats().queueLength());
        assertEquals(1, scheduler.tick(false, 20L, environment).size());
        assertEquals(1, scheduler.stats().queueLength());
    }

    @Test
    void missingRiftsAndUnloadedChunksAreDroppedInsteadOfRetained() {
        EvolutionScheduler scheduler = new EvolutionScheduler(8, 8);
        FakeEnvironment environment = new FakeEnvironment();
        UUID missing = UUID.randomUUID();
        DimensionChunkKey missingKey = new DimensionChunkKey(OVERWORLD, new ChunkPos(1, 1));
        environment.loaded.add(missingKey);
        scheduler.enqueue(new EvolutionTask(missing, missingKey));

        UUID unloaded = UUID.randomUUID();
        environment.rifts.add(unloaded);
        scheduler.enqueue(new EvolutionTask(
                unloaded, new DimensionChunkKey(OVERWORLD, new ChunkPos(2, 2))));

        assertTrue(scheduler.tick(false, 30L, environment).isEmpty());
        assertEquals(0, scheduler.stats().queueLength());
        assertEquals(1L, scheduler.stats().skipped().get(EvolutionSkipReason.RIFT_MISSING));
        assertEquals(1L, scheduler.stats().skipped().get(EvolutionSkipReason.CHUNK_UNLOADED));
    }

    @Test
    void largeRiftLoadRemainsBoundedAcrossRepeatedTicks() {
        EvolutionScheduler scheduler = new EvolutionScheduler(64, 8);
        FakeEnvironment environment = new FakeEnvironment();
        for (int index = 0; index < 10_000; index++) {
            enqueue(scheduler, environment, UUID.randomUUID(), index);
        }

        assertTimeout(Duration.ofSeconds(5), () -> {
            for (int tick = 0; tick < 200; tick++) {
                assertTrue(scheduler.tick(false, tick, environment).size() <= 64);
            }
        });
        assertEquals(10_000, scheduler.stats().queueLength());
    }

    private static DimensionChunkKey enqueue(
            EvolutionScheduler scheduler, FakeEnvironment environment, UUID rift, int chunkX) {
        DimensionChunkKey key = new DimensionChunkKey(OVERWORLD, new ChunkPos(chunkX, 0));
        environment.rifts.add(rift);
        environment.loaded.add(key);
        scheduler.enqueue(new EvolutionTask(rift, key));
        return key;
    }

    private static final class FakeEnvironment implements EvolutionEnvironment {
        private final Set<UUID> rifts = new HashSet<>();
        private final Set<DimensionChunkKey> loaded = new HashSet<>();

        @Override
        public boolean riftExists(UUID riftId) {
            return rifts.contains(riftId);
        }

        @Override
        public boolean chunkLoaded(DimensionChunkKey chunk) {
            return loaded.contains(chunk);
        }

        @Override
        public int coherence(DimensionChunkKey chunk) {
            return 50;
        }

        @Override
        public int surfaceY(DimensionChunkKey chunk, int blockX, int blockZ) {
            return 70;
        }
    }
}
