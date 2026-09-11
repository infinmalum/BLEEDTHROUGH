package com.bleedthrough.meatscape.world.maw;

import com.bleedthrough.meatscape.core.registry.MeatscapeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;

/** A scheduled, bounded food source; it is not an infection random-tick system. */
public final class NutrientMoundBlock extends Block {
    public static final BooleanProperty NOURISHED = BooleanProperty.create("nourished");
    private static final int REGROW_TICKS = 1200;

    public NutrientMoundBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NOURISHED, true));
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!state.getValue(NOURISHED)) return InteractionResult.CONSUME;
        if (!player.getInventory().add(new ItemStack(MeatscapeItems.RAW_TISSUE.get()))) {
            player.drop(new ItemStack(MeatscapeItems.RAW_TISSUE.get()), false);
        }
        level.setBlock(pos, state.setValue(NOURISHED, false), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, this, REGROW_TICKS);
        return InteractionResult.CONSUME;
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(NOURISHED)) level.setBlock(pos, state.setValue(NOURISHED, true), Block.UPDATE_CLIENTS);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NOURISHED);
    }
}
