package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.coherence.evolution.EvolutionSchedulerEvents;
import com.bleedthrough.meatscape.coherence.rift.RiftCoreBlock;
import com.bleedthrough.meatscape.coherence.rift.RiftFieldCalculator;
import com.bleedthrough.meatscape.coherence.rift.RiftFieldEvents;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.progression.BleedingService;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Meatscape.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BleedingGameTests {
    private BleedingGameTests() { }

    @GameTest(template = "empty", batch = "bleedingLifecycle")
    public static void dormantCoreActivatesAndDeletionStopsSource(GameTestHelper helper) {
        var level = helper.getLevel();
        var data = MeatscapeWorldData.get(level.getServer());
        var originalStage = data.worldStage();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var old = level.getBlockState(pos);
        var chunk = level.getChunkAt(pos);
        int coherence = MawCoherenceService.get(chunk);
        try {
            level.setBlockAndUpdate(pos, MeatscapeBlocks.RIFT_CORE.get().defaultBlockState().setValue(RiftCoreBlock.ACTIVE, false));
            var rift = data.rifts().stream().filter(value -> value.position().equals(pos)).findFirst().orElseThrow();
            helper.assertTrue(!rift.active(), "Dormant block created an active source");
            helper.assertTrue(rift.radius() == 96, "Core radius must use block units");
            helper.assertTrue(RiftFieldCalculator.contribution(rift, chunk.getPos()) == 0, "Dormant field leaked");
            helper.assertTrue(BleedingService.returned(data, Level.NETHER.location(), Level.OVERWORLD.location(), pos, 0), "Return not scheduled");
            var active = BleedingService.activateReady(data, level.getGameTime()).orElseThrow();
            RiftCoreBlock.syncLoadedState(level, active);
            EvolutionSchedulerEvents.enqueueRift(level.getServer(), active);
            helper.assertTrue(level.getBlockState(pos).getValue(RiftCoreBlock.ACTIVE), "Core did not show activation");
            helper.assertTrue(data.rifts().stream().filter(value -> value.position().equals(pos)).count() == 1,
                    "Activation duplicated source");
            RiftFieldEvents.update(level.getServer());
            helper.assertTrue(MawCoherenceService.get(chunk) > coherence, "Active core failed to diffuse at a chunk corner");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            helper.assertTrue(data.findRift(rift.id()).isEmpty(), "Removed core left a source");
        } finally {
            level.setBlockAndUpdate(pos, old);
            data.setWorldStage(originalStage);
            MawCoherenceService.set(level, chunk.getPos(), coherence);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "bleedingStateChange")
    public static void coreStateChangesKeepOneSourceAndRespectDormancy(GameTestHelper helper) {
        var level = helper.getLevel();
        var data = MeatscapeWorldData.get(level.getServer());
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var old = level.getBlockState(pos);
        try {
            var state = MeatscapeBlocks.RIFT_CORE.get().defaultBlockState();
            level.setBlockAndUpdate(pos, state);
            var rift = data.rifts().stream().filter(value -> value.position().equals(pos)).findFirst().orElseThrow();
            level.setBlockAndUpdate(pos, state.setValue(RiftCoreBlock.ACTIVE, false));
            helper.assertTrue(!data.findRift(rift.id()).orElseThrow().active(), "Block-state dormancy not persisted");
            EvolutionSchedulerEvents.rebuild(level.getServer());
            var dormant = data.findRift(rift.id()).orElseThrow();
            helper.assertTrue(com.bleedthrough.meatscape.coherence.evolution.EvolutionTaskPlanner.forRift(dormant, key -> true).isEmpty(),
                    "Rebuild scheduled a dormant source");
            level.setBlockAndUpdate(pos, state);
            helper.assertTrue(data.findRift(rift.id()).orElseThrow().active(), "Reactivation lost the source ID");
        } finally {
            level.setBlockAndUpdate(pos, old);
        }
        helper.succeed();
    }
}
