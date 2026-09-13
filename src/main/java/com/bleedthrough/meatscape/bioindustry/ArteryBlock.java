package com.bleedthrough.meatscape.bioindustry;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ArteryBlock extends BaseEntityBlock {
    public ArteryBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH)); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override @Nullable public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArteryBlockEntity(pos, state); }
    @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return level.isClientSide ? null : createTickerHelper(type, MeatscapeBlockEntities.ARTERY.get(), ArteryBlockEntity::serverTick); }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING); }
}
