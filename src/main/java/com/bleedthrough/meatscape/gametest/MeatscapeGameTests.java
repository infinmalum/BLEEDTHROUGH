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
import com.bleedthrough.meatscape.coherence.rollback.RestorationSource;
import com.bleedthrough.meatscape.coherence.rollback.RollbackJob;
import com.bleedthrough.meatscape.coherence.rollback.RollbackResult;
import com.bleedthrough.meatscape.coherence.rollback.RollbackScheduler;
import com.bleedthrough.meatscape.coherence.rollback.RollbackService;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
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

    @GameTest(template = "empty", batch = "phase5Rollback")
    public static void forwardConversionAndRollbackRepeatWithoutSnapshot(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos target = helper.absolutePos(new BlockPos(1, 1, 1));
        var safety = ChunkSafetyService.get(level, target);
        for (int cycle = 0; cycle < 2; cycle++) {
            level.setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());
            safety.setTrust(TerrainTrust.TRUSTED);
            safety.clearModified(target);
            safety.clearRestoration(target);
            helper.assertTrue(SafeEvolutionConverter.apply(level, MeatscapeWorldData.get(level.getServer()),
                    candidate(level, target.above())) == ConversionDecision.DESTRUCTIVE, "forward conversion failed");
            helper.assertTrue(safety.restorationSource(target) == RestorationSource.SOIL, "coarse source was not recorded");
            helper.assertTrue(RollbackService.inspectOrRestore(level, target, true) == RollbackResult.WOULD_RESTORE,
                    "dry-run did not find restorable terrain");
            helper.assertTrue(level.getBlockState(target).is(MeatscapeBlocks.CHANGED_STONE.get()), "dry-run changed terrain");
            helper.assertTrue(RollbackService.inspectOrRestore(level, target, false) == RollbackResult.RESTORED,
                    "rollback did not restore terrain");
            helper.assertTrue(level.getBlockState(target).is(Blocks.DIRT), "source category restored wrong terrain");
            helper.assertTrue(safety.restorationSource(target) == null, "restoration record was not cleaned");
        }

        safety.setTrust(TerrainTrust.UNKNOWN);
        BlockPos attachment = target.above();
        level.setBlockAndUpdate(attachment, Blocks.AIR.defaultBlockState());
        helper.assertTrue(SafeEvolutionConverter.apply(level, MeatscapeWorldData.get(level.getServer()),
                candidate(level, attachment)) == ConversionDecision.ATTACHMENT, "attachment fallback was not selected");
        helper.assertTrue(ChunkSafetyService.get(level, attachment).restorationSource(attachment)
                == RestorationSource.ATTACHMENT, "attachment source was not recorded");
        helper.assertTrue(RollbackService.inspectOrRestore(level, attachment, false) == RollbackResult.RESTORED,
                "attachment rollback failed");
        helper.assertTrue(level.getBlockState(attachment).isAir(), "attachment did not restore to air");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "phase5Rollback")
    public static void playerOverridesAndPermanentBlocksAreNeverOverwritten(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 1, 1));
        var safety = ChunkSafetyService.get(level, playerPos);
        safety.recordRestoration(playerPos, RestorationSource.STONE);
        level.setBlockAndUpdate(playerPos, Blocks.OAK_PLANKS.defaultBlockState());
        helper.assertTrue(RollbackService.inspectOrRestore(level, playerPos, false) == RollbackResult.PLAYER_OVERRIDE,
                "player override was not detected");
        helper.assertTrue(level.getBlockState(playerPos).is(Blocks.OAK_PLANKS), "player block was overwritten");

        BlockPos permanentPos = helper.absolutePos(new BlockPos(2, 1, 1));
        level.setBlockAndUpdate(permanentPos, MeatscapeBlocks.BASE_ANCHOR.get().defaultBlockState());
        ChunkSafetyService.get(level, permanentPos).recordRestoration(permanentPos, RestorationSource.STONE);
        helper.assertTrue(RollbackService.inspectOrRestore(level, permanentPos, false) == RollbackResult.PERMANENT,
                "permanent content was eligible for rollback");
        helper.assertTrue(level.getBlockState(permanentPos).is(MeatscapeBlocks.BASE_ANCHOR.get()),
                "permanent content was overwritten");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "phase5Rollback")
    public static void rollbackRateIsBoundedAndUnloadedChunkDoesNotLoad(GameTestHelper helper) {
        var level = helper.getLevel();
        MeatscapeWorldData data = MeatscapeWorldData.get(level.getServer());
        BlockPos first = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos second = first.east();
        for (BlockPos pos : new BlockPos[] { first, second }) {
            level.setBlockAndUpdate(pos, MeatscapeBlocks.CHANGED_STONE.get().defaultBlockState());
            ChunkSafetyService.get(level, pos).recordRestoration(pos, RestorationSource.STONE);
        }
        RollbackJob bounded = new RollbackJob(UUID.randomUUID(), level.dimension().location(), first, second, 1, false);
        data.addRollbackJob(bounded);
        RollbackScheduler scheduler = new RollbackScheduler();
        var firstTick = scheduler.tick(level.getServer(), data, 64);
        helper.assertTrue(firstTick.processed() == 1 && bounded.cursor() == 1, "per-job rate was exceeded");
        scheduler.tick(level.getServer(), data, 64);
        helper.assertTrue(level.getBlockState(first).is(Blocks.STONE) && level.getBlockState(second).is(Blocks.STONE),
                "bounded rollback did not finish safely");

        BlockPos unloaded = new BlockPos(first.getX() + 16_000, first.getY(), first.getZ());
        RollbackJob waiting = new RollbackJob(UUID.randomUUID(), level.dimension().location(), unloaded, unloaded, 4, false);
        data.addRollbackJob(waiting);
        var waitingTick = scheduler.tick(level.getServer(), data, 64);
        helper.assertTrue(waitingTick.waitingForChunk(), "unloaded job did not enter waiting state");
        helper.assertTrue(waiting.cursor() == 0, "unloaded position was skipped instead of resumable");
        helper.assertTrue(!level.hasChunkAt(unloaded), "rollback forced an unloaded chunk to load");
        data.removeRollbackJob(waiting.id());
        helper.succeed();
    }
}
