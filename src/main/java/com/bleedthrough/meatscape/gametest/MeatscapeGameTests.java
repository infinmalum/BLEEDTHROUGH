package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.world.data.WorldStage;
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
}
