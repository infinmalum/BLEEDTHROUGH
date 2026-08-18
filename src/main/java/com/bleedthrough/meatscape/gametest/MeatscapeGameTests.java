package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
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
}
