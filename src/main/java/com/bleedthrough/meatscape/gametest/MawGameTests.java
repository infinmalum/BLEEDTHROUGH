package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.world.maw.MawDimensions;
import com.bleedthrough.meatscape.world.maw.NutrientMoundBlock;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.core.registry.MeatscapeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

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

    @GameTest(template = "empty", batch = "phase82Maw")
    public static void nutrientMoundHarvestAndScheduledRegrowthAreBounded(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var mound = MeatscapeBlocks.NUTRIENT_MOUND.get();
        level.setBlockAndUpdate(pos, mound.defaultBlockState());
        var player = helper.makeMockPlayer();
        mound.use(level.getBlockState(pos), level, pos, player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                        net.minecraft.core.Direction.UP, pos, false));
        helper.assertTrue(!level.getBlockState(pos).getValue(NutrientMoundBlock.NOURISHED), "Harvest did not deplete mound");
        helper.assertTrue(player.getInventory().contains(new net.minecraft.world.item.ItemStack(MeatscapeItems.RAW_TISSUE.get())),
                "Harvest did not grant Raw Tissue");
        mound.tick(level.getBlockState(pos), level, pos, level.random);
        helper.assertTrue(level.getBlockState(pos).getValue(NutrientMoundBlock.NOURISHED), "Scheduled regrowth did not restore mound");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "phase84Wormhole")
    public static void endWormholeIsRegisteredWithoutPlaceableItem(GameTestHelper helper) {
        helper.assertTrue(MeatscapeBlocks.END_WORMHOLE.get().defaultBlockState().is(MeatscapeBlocks.END_WORMHOLE.get()),
                "End Wormhole block was not registered");
        helper.assertTrue(!ForgeRegistries.ITEMS.containsKey(ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "end_wormhole")),
                "End Wormhole must remain a natural-only entrance without a BlockItem");
        helper.succeed();
    }
}
