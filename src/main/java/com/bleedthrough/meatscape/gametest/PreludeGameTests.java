package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.rift.RiftCoreBlock;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Meatscape.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PreludeGameTests {
    private static final ResourceLocation FEATURE = ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "dormant_rift");
    private PreludeGameTests() { }

    @GameTest(template = "empty", batch = "preludeWorldgen")
    public static void naturalFeatureWritesOnlyProtoChunkAndSchedulesRegistration(GameTestHelper helper) {
        var level = helper.getLevel();
        var position = new ChunkPos(1900, 1900);
        var proto = new ProtoChunk(position, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
        proto.setStatus(ChunkStatus.CARVERS);
        var pos = position.getMiddleBlockPosition(70);
        proto.setBlockState(pos.below(), Blocks.STONE.defaultBlockState(), false);
        var region = new WorldGenRegion(level, List.of(proto), ChunkStatus.FEATURES, 0);
        var configured = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(FEATURE);
        helper.assertTrue(configured != null, "Configured feature missing");
        var plains = level.registryAccess().registryOrThrow(Registries.BIOME)
                .get(ResourceLocation.withDefaultNamespace("plains"));
        helper.assertTrue(plains.getGenerationSettings().features().stream().flatMap(set -> set.stream())
                .anyMatch(holder -> holder.is(FEATURE)), "Biome modifier did not attach the placed feature");
        int before = MeatscapeWorldData.get(level.getServer()).rifts().size();
        helper.assertTrue(configured.place(region, level.getChunkSource().getGenerator(), level.random, pos), "Natural placement failed");
        helper.assertTrue(proto.getBlockState(pos).is(MeatscapeBlocks.RIFT_CORE.get()), "No generated core");
        helper.assertTrue(!proto.getBlockState(pos).getValue(RiftCoreBlock.ACTIVE), "Generated core is active");
        helper.assertTrue(proto.getBlockTicks().hasScheduledTick(pos, MeatscapeBlocks.RIFT_CORE.get()), "Missing persisted registration tick");
        helper.assertTrue(MeatscapeWorldData.get(level.getServer()).rifts().size() == before, "Worldgen touched SavedData");
        helper.assertTrue(level.getChunkSource().getChunkNow(position.x, position.z) == null, "Fixture forced a chunk load");
        helper.assertTrue(!configured.place(region, level.getChunkSource().getGenerator(), level.random, pos), "Feature overwrote existing content");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "preludeLegacy")
    public static void runtimeFeatureRejectsLegacyTerrainAndRegistrationIsIdempotent(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var configured = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(FEATURE);
        var old = level.getBlockState(pos);
        helper.assertTrue(!configured.place(level, level.getChunkSource().getGenerator(), level.random, pos), "Runtime retro-generation allowed");
        helper.assertTrue(level.getBlockState(pos).equals(old), "Historical block changed");
        var core = (RiftCoreBlock) MeatscapeBlocks.RIFT_CORE.get();
        var data = MeatscapeWorldData.get(level.getServer());
        try {
            var state = core.defaultBlockState().setValue(RiftCoreBlock.ACTIVE, false);
            level.setBlockAndUpdate(pos, state);
            var first = data.rifts().stream().filter(rift -> rift.position().equals(pos)).findFirst().orElseThrow();
            core.tick(state, level, pos, level.random);
            core.tick(state, level, pos, level.random);
            helper.assertTrue(data.findRift(first.id()).isPresent(), "Registration changed source identity");
            helper.assertTrue(data.rifts().stream().filter(rift -> rift.position().equals(pos)).count() == 1, "Registration duplicated source");
        } finally { level.setBlockAndUpdate(pos, old); }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "preludeAnimals")
    public static void animalGoalDoesNotLeavePermanentFlagsOrDuplicateOnRejoin(GameTestHelper helper) {
        var animal = helper.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(1, 2, 1));
        try {
            var event = new net.minecraftforge.event.entity.EntityJoinLevelEvent(animal, helper.getLevel());
            com.bleedthrough.meatscape.progression.PreludeAnimalGoal.join(event);
            com.bleedthrough.meatscape.progression.PreludeAnimalGoal.join(event);
            var goals = animal.goalSelector.getAvailableGoals().stream()
                    .filter(goal -> goal.getGoal() instanceof com.bleedthrough.meatscape.progression.PreludeAnimalGoal).toList();
            helper.assertTrue(goals.size() == 1, "Duplicate prelude goal after rejoin");
            var goal = goals.get(0).getGoal();
            helper.assertTrue(!goal.canUse(), "Animal paused without an observed prelude");
            for (boolean noAi : new boolean[] {false, true}) {
                animal.setNoAi(noAi);
                goal.start();
                goal.tick();
                goal.stop();
                helper.assertTrue(animal.isNoAi() == noAi, "Prelude changed the permanent NoAI flag");
            }
        } finally { animal.discard(); }
        helper.succeed();
    }
}
