package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.coherence.evolution.EvolutionCandidate;
import com.bleedthrough.meatscape.coherence.rift.RiftFieldEvents;
import com.bleedthrough.meatscape.coherence.rift.RiftRecord;
import com.bleedthrough.meatscape.coherence.rollback.RestorationSource;
import com.bleedthrough.meatscape.coherence.rollback.RollbackResult;
import com.bleedthrough.meatscape.coherence.rollback.RollbackService;
import com.bleedthrough.meatscape.coherence.thermal.Cauterization;
import com.bleedthrough.meatscape.coherence.thermal.ThermalRules;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.core.registry.MeatscapeItems;
import com.bleedthrough.meatscape.safety.ChunkSafetyService;
import com.bleedthrough.meatscape.safety.ConversionDecision;
import com.bleedthrough.meatscape.safety.ProtectedRegion;
import com.bleedthrough.meatscape.safety.SafeEvolutionConverter;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.world.data.ThermalProfile;
import java.util.Collections;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Meatscape.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThermalGameTests {
    private static final ResourceLocation ICE = ResourceLocation.withDefaultNamespace("ice_spikes");
    private static final ResourceLocation PLAINS = ResourceLocation.withDefaultNamespace("plains");
    private ThermalGameTests() { }

    /** Change only the sampled quart Y plane, restoring it in finally after every fixture. */
    static void biomes(ServerLevel level, LevelChunk chunk, ThermalProfile profile) {
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> y == 16
                ? registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, profile.biomes().get((z & 3) * 4 + (x & 3))))
                : chunk.getNoiseBiome(x, y, z), level.getChunkSource().randomState().sampler());
    }

    @GameTest(template = "empty", batch = "thermalCold")
    public static void coldCapsLoadedAndUnloadedFieldsWithoutForcingChunks(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var chunk = level.getChunkAt(pos);
        var original = ThermalRules.sample(chunk);
        var world = MeatscapeWorldData.get(level.getServer());
        var remote = new ChunkPos(2500, 2500);
        var key = ThermalRules.key(level, remote);
        var rift = new RiftRecord(UUID.randomUUID(), level.dimension().location(), remote.getMiddleBlockPosition(64), 16, 100, 0, -1);
        boolean paused = world.isPaused();
        try {
            world.setPaused(false);
            biomes(level, chunk, new ThermalProfile(Collections.nCopies(16, ICE)));
            helper.assertTrue(MawCoherenceService.set(level, chunk.getPos(), 100) == 15, "Loaded cold cap bypassed");
            var loadedKey = ThermalRules.key(level, chunk.getPos());
            world.addPendingCoherence(loadedKey, 100);
            RiftFieldEvents.chunkLoad(new net.minecraftforge.event.level.ChunkEvent.Load(chunk, false));
            helper.assertTrue(MawCoherenceService.get(chunk) == 15 && world.pendingCoherence(loadedKey) == 0,
                    "Load-time merge escaped the actual biome cap");
            world.rememberThermalProfile(key, new ThermalProfile(Collections.nCopies(16, ICE)));
            world.addPendingCoherence(key, 100);
            world.addRift(rift);
            RiftFieldEvents.update(level.getServer());
            helper.assertTrue(world.pendingCoherence(key) <= 15, "Unloaded cap bypassed");
            world.suppress(key);
            RiftFieldEvents.update(level.getServer());
            helper.assertTrue(world.pendingCoherence(key) == 0, "Unloaded suppression bypassed");
            helper.assertTrue(level.getChunkSource().getChunkNow(remote.x, remote.z) == null, "Thermal sampling forced chunk load");
            var unknown = new ChunkPos(2600, 2600);
            ThermalRules.cap(level, unknown);
            helper.assertTrue(level.getChunkSource().getChunkNow(unknown.x, unknown.z) == null, "Biome source lookup generated terrain");
        } finally {
            world.removeRift(rift.id());
            world.consumePendingCoherence(key);
            world.clearSuppression(key);
            biomes(level, chunk, original);
            MawCoherenceService.set(level, chunk.getPos(), 0);
            world.setPaused(paused);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "thermalMixed")
    public static void mixedBiomeBoundaryFreezesOnlyColdColumnsAndReducesPumpYield(GameTestHelper helper) {
        var level = helper.getLevel();
        var chunk = level.getChunkAt(helper.absolutePos(new BlockPos(1, 2, 1)));
        var original = ThermalRules.sample(chunk);
        var ids = new java.util.ArrayList<ResourceLocation>();
        for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) ids.add(x < 2 ? ICE : PLAINS);
        var cold = chunk.getPos().getBlockAt(2, 70, 2);
        var warm = chunk.getPos().getBlockAt(14, 70, 2);
        var before = level.getBlockState(cold);
        try {
            biomes(level, chunk, new ThermalProfile(ids));
            helper.assertTrue(ThermalRules.cap(level, chunk) == 58, "Mixed boundary cap incorrect");
            helper.assertTrue(ThermalRules.frozen(level, cold) && !ThermalRules.frozen(level, warm), "Boundary columns incorrect");
            var world = MeatscapeWorldData.get(level.getServer());
            level.setBlockAndUpdate(cold, Blocks.STONE.defaultBlockState());
            var candidate = new EvolutionCandidate(UUID.randomUUID(), ThermalRules.key(level, chunk.getPos()), cold.above());
            helper.assertTrue(SafeEvolutionConverter.apply(level, world, candidate) == ConversionDecision.SKIP_NOT_REPLACEABLE,
                    "Frozen column still grows");
            helper.assertTrue(level.getBlockState(cold).is(Blocks.STONE), "Cold natural terrain rewritten");
            level.setBlockAndUpdate(cold, MeatscapeBlocks.CHANGED_STONE.get().defaultBlockState());
            ChunkSafetyService.get(level, cold).recordRestoration(cold, RestorationSource.STONE);
            helper.assertTrue(RollbackService.inspectOrRestore(level, cold, false) == RollbackResult.RESTORED
                    && level.getBlockState(cold).is(Blocks.STONE), "Freezing destroyed the safe restoration path");
            level.setBlockAndUpdate(cold, MeatscapeBlocks.HEART_PUMP.get().defaultBlockState());
            var player = helper.makeMockPlayer();
            var held = new ItemStack(MeatscapeItems.RAW_TISSUE.get(), 7);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            var hit = new BlockHitResult(Vec3.atCenterOf(cold), Direction.UP, cold, false);
            MeatscapeBlocks.HEART_PUMP.get().use(level.getBlockState(cold), level, cold, player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(held.getCount() == 7 && !player.getInventory().contains(new ItemStack(MeatscapeItems.COLLAGEN.get())), "Cold pump consumed an incomplete batch");
            held.grow(1);
            MeatscapeBlocks.HEART_PUMP.get().use(level.getBlockState(cold), level, cold, player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(held.isEmpty() && player.getInventory().countItem(MeatscapeItems.COLLAGEN.get()) == 1, "Cold pump yield incorrect");
        } finally {
            level.setBlockAndUpdate(cold, before);
            biomes(level, chunk, original);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "thermalBurn")
    public static void deliberateCauterizationCostsDurabilityLeavesScarAndStopsGrowth(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var world = MeatscapeWorldData.get(level.getServer());
        var safety = ChunkSafetyService.get(level, pos);
        var previous = level.getBlockState(pos);
        boolean paused = world.isPaused();
        try {
            world.setPaused(false);
            level.setBlockAndUpdate(pos, MeatscapeBlocks.DERMAL_SOIL.get().defaultBlockState());
            safety.clearModified(pos);
            safety.recordRestoration(pos, RestorationSource.SOIL);
            MawCoherenceService.set(level, new ChunkPos(pos), 50);
            int before = MawCoherenceService.get(level, new ChunkPos(pos));
            var player = helper.makeMockPlayer();
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var event = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
            Cauterization.interact(event);
            helper.assertTrue(player.getMainHandItem().getDamageValue() == 0
                    && level.getBlockState(pos).is(MeatscapeBlocks.DERMAL_SOIL.get()), "DENY protection ignored");
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DEFAULT);
            Cauterization.interact(event);
            helper.assertTrue(event.isCanceled(), "Experiment fell through to ordinary fire");
            helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Experiment did not charge durability");
            helper.assertTrue(level.getBlockState(pos).is(MeatscapeBlocks.CHARRED_SCAR.get()), "No permanent scar");
            helper.assertTrue(MawCoherenceService.get(level, new ChunkPos(pos)) == Math.max(0, before - 10), "No coherence reduction");
            helper.assertTrue(ThermalRules.suppressed(level, new ChunkPos(pos)), "No suppression");
            safety.recordRestoration(pos, RestorationSource.SOIL); // Even a stale record cannot erase history.
            helper.assertTrue(RollbackService.inspectOrRestore(level, pos, false) == RollbackResult.PERMANENT, "Scar rolled back");
            helper.assertTrue(!Cauterization.apply(level, pos), "Repeated scar burning is free suppression");
        } finally {
            safety.clearRestoration(pos);
            world.clearSuppression(ThermalRules.key(level, new ChunkPos(pos)));
            level.setBlockAndUpdate(pos, previous);
            world.setPaused(paused);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "thermalSafety")
    public static void cauterizationRespectsPlayerOverridesProtectionAndRemovableFilm(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var world = MeatscapeWorldData.get(level.getServer());
        var safety = ChunkSafetyService.get(level, pos);
        var region = new ProtectedRegion(UUID.randomUUID(), level.dimension().location(), pos, pos, null);
        var before = level.getBlockState(pos);
        boolean paused = world.isPaused();
        try {
            world.setPaused(false);
            level.setBlockAndUpdate(pos, MeatscapeBlocks.DERMAL_SOIL.get().defaultBlockState());
            safety.clearRestoration(pos);
            helper.assertTrue(!Cauterization.apply(level, pos), "Unknown/player-placed tissue burned");
            safety.recordRestoration(pos, RestorationSource.SOIL);
            safety.markModified(pos);
            helper.assertTrue(!Cauterization.apply(level, pos), "Player provenance ignored");
            safety.clearModified(pos);
            world.addProtectedRegion(region);
            helper.assertTrue(!Cauterization.apply(level, pos), "Protected body burned");
            level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
            helper.assertTrue(!Cauterization.apply(level, pos), "Container burned");
            level.setBlockAndUpdate(pos, MeatscapeBlocks.DERMAL_FILM.get().defaultBlockState());
            safety.recordRestoration(pos, RestorationSource.ATTACHMENT);
            world.setPaused(true);
            helper.assertTrue(!Cauterization.apply(level, pos), "Pause ignored");
            world.setPaused(false);
            helper.assertTrue(Cauterization.apply(level, pos) && level.getBlockState(pos).isAir(), "Removable film left solid scar in protected build");
        } finally {
            world.removeProtectedRegion(region.id());
            world.clearSuppression(ThermalRules.key(level, new ChunkPos(pos)));
            safety.clearRestoration(pos);
            safety.clearModified(pos);
            level.setBlockAndUpdate(pos, before);
            world.setPaused(paused);
        }
        helper.succeed();
    }
}
