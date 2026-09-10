package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.world.maw.MawDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runtime data-pack verification for the minimum 8.1 dimension contract. */
@GameTestHolder(Meatscape.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MawGameTests {
    private MawGameTests() {
    }

    @GameTest(template = "empty", batch = "phase81Maw")
    public static void mawDimensionLoadsWithExpectedBuildRange(GameTestHelper helper) {
        var maw = helper.getLevel().getServer().getLevel(MawDimensions.MAW);
        helper.assertTrue(maw != null, "The Maw dimension was not registered from its data pack");
        helper.assertTrue(maw != null && maw.getMinBuildHeight() == -64 && maw.getMaxBuildHeight() == 320,
                "The Maw build range is not -64..319");
        helper.assertTrue(maw != null && maw.getBiome(new BlockPos(0, 64, 0)).unwrapKey()
                        .map(key -> key.location().equals(MawDimensions.MAW_ID.withPath("subdermal_expanse"))).orElse(false),
                "The Maw is not using the fixed Subdermal Expanse placeholder biome");
        helper.succeed();
    }
}
