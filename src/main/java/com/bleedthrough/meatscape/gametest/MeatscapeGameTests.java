package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.coherence.rift.RiftFieldEvents;
import com.bleedthrough.meatscape.coherence.rift.RiftRecord;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.world.data.WorldStage;
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
}
