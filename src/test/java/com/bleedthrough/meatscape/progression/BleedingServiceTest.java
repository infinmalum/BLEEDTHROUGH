package com.bleedthrough.meatscape.progression;

import static org.junit.jupiter.api.Assertions.*;

import com.bleedthrough.meatscape.coherence.evolution.EvolutionTaskPlanner;
import com.bleedthrough.meatscape.coherence.rift.RiftFieldCalculator;
import com.bleedthrough.meatscape.coherence.rift.RiftRecord;
import com.bleedthrough.meatscape.core.migration.DataSchema;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.world.data.WorldStage;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BleedingServiceTest {
    private static final ResourceLocation OVERWORLD = BleedingService.OVERWORLD;
    private static final ResourceLocation NETHER = BleedingService.NETHER;
    private static final ResourceLocation END = ResourceLocation.withDefaultNamespace("the_end");
    private static RiftRecord dormant(int x) {
        return new RiftRecord(UUID.randomUUID(), OVERWORLD, new BlockPos(x, 64, 8),
                96, 24, 0, RiftRecord.PERMANENT, false);
    }

    @Test void dormantSourceSurvivesReloadWithoutFieldOrQueuedWork() {
        RiftRecord rift = RiftRecord.load(dormant(8).save());
        assertFalse(rift.active());
        assertEquals(0, RiftFieldCalculator.contribution(rift, new ChunkPos(0, 0)));
        assertTrue(EvolutionTaskPlanner.forRift(rift, key -> true).isEmpty());
        assertTrue(RiftFieldCalculator.contribution(rift.withActive(true), new ChunkPos(0, 0)) > 0);
    }

    @Test void versionFourPreservesActiveSourcesAndDoesNotInventAPendingEvent() {
        CompoundTag oldRift = dormant(8).save();
        oldRift.remove("Active");
        ListTag rifts = new ListTag();
        rifts.add(oldRift);
        CompoundTag oldWorld = new CompoundTag();
        oldWorld.putInt("SchemaVersion", 4);
        oldWorld.put("Rifts", rifts);
        MeatscapeWorldData migrated = MeatscapeWorldData.load(oldWorld);
        assertEquals(DataSchema.WORLD_CURRENT, migrated.schemaVersion());
        assertTrue(migrated.rifts().iterator().next().active());
        assertTrue(migrated.bleedingOrigin().isEmpty());
        assertTrue(migrated.isDirty());
    }

    @Test void onlyNetherReturnSchedulesAndFirstPlayerWins() {
        var data = new MeatscapeWorldData();
        assertFalse(BleedingService.returned(data, END, OVERWORLD, BlockPos.ZERO, 20));
        assertFalse(BleedingService.returned(data, OVERWORLD, NETHER, BlockPos.ZERO, 20));
        assertTrue(BleedingService.returned(data, NETHER, OVERWORLD, BlockPos.ZERO, 20));
        assertFalse(BleedingService.returned(data, NETHER, OVERWORLD, new BlockPos(900, 64, 0), 40));
        assertEquals(BlockPos.ZERO, data.bleedingOrigin().orElseThrow());
        assertEquals(20, data.bleedingDelay());
    }

    @Test void pauseReloadAndExactlyOnceActivationPreserveProgress() {
        var data = new MeatscapeWorldData();
        RiftRecord rift = dormant(8);
        data.addRift(rift);
        data.scheduleBleeding(BlockPos.ZERO, 2);
        assertFalse(data.tickBleedingDelay());
        data.setPaused(true);
        assertFalse(data.tickBleedingDelay());
        var resumed = MeatscapeWorldData.load(data.save(new CompoundTag()));
        assertEquals(1, resumed.bleedingDelay());
        assertTrue(BleedingService.activateReady(resumed, 10).isEmpty());
        resumed.setPaused(false);
        assertTrue(resumed.tickBleedingDelay());
        assertEquals(rift.id(), BleedingService.activateReady(resumed, 10).orElseThrow().id());
        assertEquals(WorldStage.BLEEDING, resumed.worldStage());
        assertTrue(resumed.findRift(rift.id()).orElseThrow().active());
        assertTrue(resumed.spatialIndex().at(OVERWORLD, new ChunkPos(0, 0)).get(0).active());
        var completed = MeatscapeWorldData.load(resumed.save(new CompoundTag()));
        assertTrue(BleedingService.activateReady(completed, 20).isEmpty());
        assertFalse(completed.scheduleBleeding(BlockPos.ZERO, 0));
    }

    @Test void missingRemovedExpiredAndForeignSourcesCannotConsumeTheEvent() {
        var data = new MeatscapeWorldData();
        data.scheduleBleeding(BlockPos.ZERO, 0);
        RiftRecord removed = dormant(8);
        data.addRift(removed);
        data.removeRift(removed.id());
        data.addRift(dormant(600));
        data.addRift(new RiftRecord(UUID.randomUUID(), NETHER, BlockPos.ZERO, 96, 24, 0, -1, false));
        data.addRift(new RiftRecord(UUID.randomUUID(), OVERWORLD, BlockPos.ZERO, 96, 24, 0, 1, false));
        assertTrue(BleedingService.activateReady(data, 10).isEmpty());
        assertEquals(WorldStage.DORMANT, data.worldStage());
        assertTrue(data.bleedingOrigin().isPresent());
        RiftRecord nearer = dormant(32);
        data.addRift(dormant(300));
        data.addRift(nearer);
        assertEquals(nearer.id(), BleedingService.activateReady(data, 10).orElseThrow().id());
        assertEquals(1, data.rifts().stream().filter(RiftRecord::active).count());
    }
}
