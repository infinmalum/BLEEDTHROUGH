package com.bleedthrough.meatscape.bioindustry;

import com.bleedthrough.meatscape.core.registry.MeatscapeItems;
import com.bleedthrough.meatscape.progression.KnowledgeObservation;
import com.bleedthrough.meatscape.progression.PlayerKnowledge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Minimal hand-fed processor: two tissue become one collagen without external machinery. */
public final class HeartPumpBlock extends BaseEntityBlock {
    public HeartPumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(MeatscapeItems.RAW_TISSUE.get())) return InteractionResult.PASS;
        if (!level.isClientSide) {
            int cost = com.bleedthrough.meatscape.coherence.thermal.ThermalRules.frozen(
                    (net.minecraft.server.level.ServerLevel) level, pos) ? 8 : 2;
            if (held.getCount() < cost) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.meatscape.pump_cost", cost), true);
                return InteractionResult.CONSUME;
            }
            if (!(level.getBlockEntity(pos) instanceof HeartPumpBlockEntity pump) || pump.capacity() - pump.hematic() < HeartPumpBlockEntity.TISSUE_YIELD) return InteractionResult.CONSUME;
            held.shrink(cost); pump.addHematic(HeartPumpBlockEntity.TISSUE_YIELD);
            ItemStack result = new ItemStack(MeatscapeItems.COLLAGEN.get());
            if (!player.getInventory().add(result)) player.drop(result, false);
            if (player instanceof ServerPlayer serverPlayer) {
                PlayerKnowledge.observe(serverPlayer, KnowledgeObservation.BIOINDUSTRY);
                serverPlayer.swing(hand, true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HeartPumpBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return level.isClientSide ? null : createTickerHelper(type, MeatscapeBlockEntities.HEART_PUMP.get(), HeartPumpBlockEntity::serverTick); }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING); }
}
