package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.architecture.RegenerativeMembraneBlock;
import com.bleedthrough.meatscape.architecture.RegenerativeMembraneBlockEntity;
import com.bleedthrough.meatscape.bioindustry.ArteryBlockEntity;
import com.bleedthrough.meatscape.coherence.evolution.EvolutionCandidate;
import com.bleedthrough.meatscape.coherence.rollback.RestorationSource;
import com.bleedthrough.meatscape.coherence.rollback.RollbackResult;
import com.bleedthrough.meatscape.coherence.rollback.RollbackService;
import com.bleedthrough.meatscape.coherence.thermal.ThermalRules;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.core.registry.MeatscapeItems;
import com.bleedthrough.meatscape.safety.ChunkSafetyService;
import com.bleedthrough.meatscape.safety.ConversionDecision;
import com.bleedthrough.meatscape.safety.SafeEvolutionConverter;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.world.data.ThermalProfile;
import java.util.Collections;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Meatscape.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LivingArchitectureGameTests {
    private static final ResourceLocation ICE = ResourceLocation.withDefaultNamespace("ice_spikes");
    private LivingArchitectureGameTests() { }

    @GameTest(template = "empty", batch = "livingMembraneHealing")
    public static void nutritionPauseColdAndReloadControlHealing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var chunk = level.getChunkAt(pos);
        ThermalProfile originalBiomes = ThermalRules.sample(chunk);
        var world = MeatscapeWorldData.get(level.getServer());
        boolean paused = world.isPaused();
        try {
            world.setPaused(false);
            level.setBlockAndUpdate(pos, MeatscapeBlocks.REGENERATIVE_MEMBRANE.get().defaultBlockState());
            var block = (RegenerativeMembraneBlock) MeatscapeBlocks.REGENERATIVE_MEMBRANE.get();
            var membrane = (RegenerativeMembraneBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(membrane != null && membrane.nutrition() == 0, "Membrane BlockEntity missing");

            var player = helper.makeMockPlayer();
            var paste = new ItemStack(MeatscapeItems.NUTRIENT_PASTE.get(), 5);
            player.setItemInHand(InteractionHand.MAIN_HAND, paste);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            for (int i = 0; i < 5; i++) block.use(level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(membrane.nutrition() == 4 && paste.getCount() == 1, "Capacity or feed cost is not bounded");
            helper.assertTrue(block.getAnalogOutputSignal(level.getBlockState(pos), level, pos) == 15, "Comparator does not report nutrition");

            block.onBlockExploded(level.getBlockState(pos), level, pos, null);
            helper.assertTrue(level.getBlockState(pos).getValue(RegenerativeMembraneBlock.WOUNDED), "Explosion did not wound wall");
            world.setPaused(true);
            tick(level, pos, membrane, 40);
            helper.assertTrue(membrane.healProgress() == 0 && membrane.nutrition() == 4, "Pause consumed healing time or nutrition");
            world.setPaused(false);

            ThermalGameTests.biomes(level, chunk, new ThermalProfile(Collections.nCopies(16, ICE)));
            tick(level, pos, membrane, 240);
            helper.assertTrue(membrane.healProgress() == 0 && membrane.nutrition() == 4, "White Sanctuary did not hibernate wall");
            ThermalGameTests.biomes(level, chunk, originalBiomes);
            tick(level, pos, membrane, 73);

            CompoundTag saved = membrane.saveWithoutMetadata();
            var reloaded = new RegenerativeMembraneBlockEntity(pos, level.getBlockState(pos));
            reloaded.load(saved);
            reloaded.setLevel(level);
            level.removeBlockEntity(pos);
            level.setBlockEntity(reloaded);
            helper.assertTrue(reloaded.healProgress() == 73 && reloaded.nutrition() == 4, "Unload/reload state was not preserved");
            tick(level, pos, reloaded, RegenerativeMembraneBlockEntity.HEAL_TICKS - 73);
            helper.assertTrue(!level.getBlockState(pos).getValue(RegenerativeMembraneBlock.WOUNDED), "Wall did not heal after remaining loaded ticks");
            helper.assertTrue(reloaded.nutrition() == 3 && reloaded.healProgress() == 0, "A completed heal must consume exactly one nutrition");

            CompoundTag malformed = new CompoundTag();
            malformed.putInt("Nutrition", Integer.MAX_VALUE);
            malformed.putInt("HealProgress", Integer.MAX_VALUE);
            reloaded.load(malformed);
            helper.assertTrue(reloaded.nutrition() == 4 && reloaded.healProgress() == 199, "Malformed NBT was not clamped");
        } finally {
            ThermalGameTests.biomes(level, chunk, originalBiomes);
            world.setPaused(paused);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "livingMembraneSafety")
    public static void membraneIsAnOrganCoreButCanStillBeDeliberatelyRemoved(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var world = MeatscapeWorldData.get(level.getServer());
        try {
            level.setBlockAndUpdate(pos, MeatscapeBlocks.REGENERATIVE_MEMBRANE.get().defaultBlockState());
            var safety = ChunkSafetyService.get(level, pos);
            safety.recordRestoration(pos, RestorationSource.STONE); // stale data must not rewrite an organ component
            helper.assertTrue(RollbackService.inspectOrRestore(level, pos, false) == RollbackResult.PERMANENT,
                    "Rollback erased Living Architecture");
            var candidate = new EvolutionCandidate(UUID.randomUUID(), ThermalRules.key(level, level.getChunkAt(pos).getPos()), pos.above());
            helper.assertTrue(SafeEvolutionConverter.apply(level, world, candidate) == ConversionDecision.SKIP_ABSOLUTE,
                    "Evolution rewrote Living Architecture");
            helper.assertTrue(level.getBlockEntity(pos) instanceof RegenerativeMembraneBlockEntity, "Component lost its BlockEntity");
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 4.0F,
                    net.minecraft.world.level.Level.ExplosionInteraction.TNT);
            helper.assertTrue(level.getBlockState(pos).is(MeatscapeBlocks.REGENERATIVE_MEMBRANE.get())
                    && level.getBlockState(pos).getValue(RegenerativeMembraneBlock.WOUNDED), "First real explosion did not leave a wound");
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 4.0F,
                    net.minecraft.world.level.Level.ExplosionInteraction.TNT);
            helper.assertTrue(level.getBlockState(pos).isAir(), "Unrepaired wall became an immortal blast shield");
            level.setBlockAndUpdate(pos, MeatscapeBlocks.REGENERATIVE_MEMBRANE.get().defaultBlockState());
            helper.assertTrue(level.destroyBlock(pos, true), "Players cannot deliberately remove the component");
            helper.assertTrue(level.getBlockState(pos).isAir() && level.getBlockEntity(pos) == null, "Removal retained runtime state");
        } finally {
            ChunkSafetyService.get(level, pos).clearRestoration(pos);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "phase87LivingNetwork")
    public static void arteryFeedsMembraneBoundedlyAndPersistsItsBuffer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos arteryPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos membranePos = arteryPos.east();
        level.setBlockAndUpdate(arteryPos, MeatscapeBlocks.ARTERY.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        level.setBlockAndUpdate(membranePos, MeatscapeBlocks.REGENERATIVE_MEMBRANE.get().defaultBlockState());
        var artery = (ArteryBlockEntity) level.getBlockEntity(arteryPos);
        var membrane = (RegenerativeMembraneBlockEntity) level.getBlockEntity(membranePos);
        helper.assertTrue(artery != null && membrane != null, "Living network BlockEntity missing");
        artery.addHematic(300);
        for (int tick = 0; tick < 5; tick++) ArteryBlockEntity.serverTick(level, arteryPos, level.getBlockState(arteryPos), artery);
        helper.assertTrue(artery.hematic() == 50 && membrane.nutrition() == 1, "Artery transfer did not conserve the 250 mB nutrition cost");
        CompoundTag saved = membrane.saveWithoutMetadata();
        var reloaded = new RegenerativeMembraneBlockEntity(membranePos, level.getBlockState(membranePos));
        reloaded.load(saved);
        helper.assertTrue(reloaded.nutrition() == 1, "Membrane nutrition did not survive serialization");
        level.removeBlock(membranePos, false);
        ArteryBlockEntity.serverTick(level, arteryPos, level.getBlockState(arteryPos), artery);
        helper.assertTrue(artery.hematic() == 50, "Disconnected network moved or lost Hematic volume");
        helper.succeed();
    }

    private static void tick(ServerLevel level, BlockPos pos, RegenerativeMembraneBlockEntity membrane, int count) {
        for (int i = 0; i < count; i++) {
            RegenerativeMembraneBlockEntity.serverTick(level, pos, level.getBlockState(pos), membrane);
        }
    }
}
