package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.coherence.evolution.EvolutionSchedulerEvents;
import com.bleedthrough.meatscape.coherence.rift.RiftFieldEvents;
import com.bleedthrough.meatscape.coherence.rift.RiftRecord;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.world.data.WorldStage;
import com.bleedthrough.meatscape.safety.ChunkSafetyService;
import com.bleedthrough.meatscape.safety.ConversionDecision;
import com.bleedthrough.meatscape.safety.ProtectedRegion;
import com.bleedthrough.meatscape.safety.ProvenanceService;
import com.bleedthrough.meatscape.safety.SafeEvolutionConverter;
import com.bleedthrough.meatscape.safety.TerrainTrust;
import com.bleedthrough.meatscape.coherence.evolution.EvolutionCandidate;
import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Minimal Forge loading smoke test for the Phase 0 foundation. */
@GameTestHolder(Meatscape.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MeatscapeGameTests {
    private MeatscapeGameTests() {
    }

    @GameTest(template = "empty")
    public static void foundationLoads(GameTestHelper helper) {
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void coherenceCapabilityPersistsInLoadedChunk(GameTestHelper helper) {
        var chunk = helper.getLevel().getChunkAt(helper.absolutePos(BlockPos.ZERO));
        int original = MawCoherenceService.get(chunk);
        MawCoherenceService.set(helper.getLevel(), chunk.getPos(), 42);
        helper.assertTrue(MawCoherenceService.get(chunk) == 42, "Chunk coherence did not retain its value");
        MawCoherenceService.set(helper.getLevel(), chunk.getPos(), original);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void worldDataIsAvailableOnDedicatedServer(GameTestHelper helper) {
        MeatscapeWorldData data = MeatscapeWorldData.get(helper.getLevel().getServer());
        helper.assertTrue(data.worldStage() == WorldStage.DORMANT, "Unexpected initial world stage");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "riftField")
    public static void riftDiffusesIntoLoadedChunkAndDeletionStopsIt(GameTestHelper helper) {
        var level = helper.getLevel();
        var chunk = level.getChunkAt(helper.absolutePos(BlockPos.ZERO));
        int original = MawCoherenceService.get(chunk);
        MeatscapeWorldData data = MeatscapeWorldData.get(level.getServer());
        RiftRecord rift = new RiftRecord(
                UUID.randomUUID(),
                level.dimension().location(),
                chunk.getPos().getMiddleBlockPosition(64),
                RiftRecord.MIN_RADIUS,
                100,
                level.getGameTime(),
                RiftRecord.PERMANENT);
        data.addRift(rift);

        RiftFieldEvents.update(level.getServer());
        int diffused = MawCoherenceService.get(chunk);
        helper.assertTrue(diffused > original, "Rift did not increase loaded chunk coherence");

        data.removeRift(rift.id());
        RiftFieldEvents.update(level.getServer());
        helper.assertTrue(MawCoherenceService.get(chunk) == diffused, "Deleted Rift continued diffusing");
        MawCoherenceService.set(level, chunk.getPos(), original);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "evolutionScheduler", timeoutTicks = 40)
    public static void evolutionSchedulerRecordsCandidatesWithoutChangingBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos observed = helper.absolutePos(BlockPos.ZERO);
        var originalState = level.getBlockState(observed);
        var chunk = level.getChunkAt(observed);
        int originalCoherence = MawCoherenceService.get(chunk);
        MeatscapeWorldData data = MeatscapeWorldData.get(level.getServer());
        RiftRecord rift = new RiftRecord(
                UUID.randomUUID(),
                level.dimension().location(),
                chunk.getPos().getMiddleBlockPosition(64),
                RiftRecord.MIN_RADIUS,
                100,
                level.getGameTime(),
                RiftRecord.PERMANENT);
        MawCoherenceService.set(level, chunk.getPos(), 50);
        data.addRift(rift);
        var scheduler = EvolutionSchedulerEvents.get(level.getServer());
        long processedBefore = scheduler.stats().totalProcessed();
        EvolutionSchedulerEvents.enqueueRift(level.getServer(), rift);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(
                    scheduler.stats().totalProcessed() > processedBefore,
                    "Evolution Scheduler did not record a candidate");
            helper.assertTrue(
                    level.getBlockState(observed).equals(originalState),
                    "Empty Evolution Scheduler changed a block");
            data.removeRift(rift.id());
            scheduler.clear();
            MawCoherenceService.set(level, chunk.getPos(), originalCoherence);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "phase4Safety")
    public static void trustedNaturalTerrainConvertsButPlayerBuildDoesNot(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos target = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        var safety = ChunkSafetyService.get(level, target);
        safety.setTrust(TerrainTrust.TRUSTED);
        safety.clearModified(target);
        EvolutionCandidate candidate = candidate(level, target.above());
        helper.assertTrue(SafeEvolutionConverter.apply(level, MeatscapeWorldData.get(level.getServer()), candidate)
                == ConversionDecision.DESTRUCTIVE, "trusted natural stone was not converted");

        for (var playerBlock : new net.minecraft.world.level.block.Block[] {
                Blocks.STONE, Blocks.OAK_LOG, Blocks.DIRT, Blocks.ICE }) {
            level.setBlockAndUpdate(target, playerBlock.defaultBlockState());
            ProvenanceService.markModified(level, target, level.getBlockState(target));
            helper.assertTrue(SafeEvolutionConverter.apply(level, MeatscapeWorldData.get(level.getServer()), candidate)
                    == ConversionDecision.ATTACHMENT, "player block was structurally replaced: " + playerBlock);
            helper.assertTrue(level.getBlockState(target).is(playerBlock), "player structure changed: " + playerBlock);
            level.setBlockAndUpdate(target.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(target.north(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(target.south(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(target.east(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(target.west(), Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "phase4Safety")
    public static void blockEntitiesAndProtectedSettlementsNeverConvertStructurally(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos target = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
        ChunkSafetyService.get(level, target).setTrust(TerrainTrust.TRUSTED);
        helper.assertTrue(SafeEvolutionConverter.apply(level, MeatscapeWorldData.get(level.getServer()), candidate(level, target.above()))
                == ConversionDecision.SKIP_ABSOLUTE, "BlockEntity was not absolutely protected");
        helper.assertTrue(level.getBlockState(target).is(Blocks.CHEST), "container changed");

        level.setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());
        UUID regionId = UUID.randomUUID();
        MeatscapeWorldData world = MeatscapeWorldData.get(level.getServer());
        world.addProtectedRegion(new ProtectedRegion(regionId, level.dimension().location(), target, target, null));
        helper.assertTrue(SafeEvolutionConverter.apply(level, world, candidate(level, target.above()))
                == ConversionDecision.ATTACHMENT, "protected terrain did not use attachment fallback");
        helper.assertTrue(level.getBlockState(target).is(Blocks.DIRT), "protected settlement changed structurally");
        world.removeProtectedRegion(regionId);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "phase4Safety")
    public static void legacyUnknownAndSimulatedBulkMovesUseFallback(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos target = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(target, Blocks.ICE.defaultBlockState());
        var safety = ChunkSafetyService.get(level, target);
        safety.setTrust(TerrainTrust.UNKNOWN);
        helper.assertTrue(SafeEvolutionConverter.apply(level, MeatscapeWorldData.get(level.getServer()), candidate(level, target.above()))
                == ConversionDecision.ATTACHMENT, "legacy unknown terrain converted destructively");
        safety.setTrust(TerrainTrust.TRUSTED);
        ProvenanceService.markUntrusted(level, target, target.offset(1, 0, 0));
        helper.assertTrue(safety.isModified(target), "bulk movement did not mark provenance");
        helper.assertTrue(level.getBlockState(target).is(Blocks.ICE), "bulk-moved structure changed");
        helper.succeed();
    }

    private static EvolutionCandidate candidate(net.minecraft.server.level.ServerLevel level, BlockPos surface) {
        return new EvolutionCandidate(UUID.randomUUID(),
                new DimensionChunkKey(level.dimension().location(), level.getChunkAt(surface).getPos()), surface);
    }
}
