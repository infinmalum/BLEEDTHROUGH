package com.bleedthrough.meatscape.bioindustry;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class ArteryBlockEntity extends HematicNodeBlockEntity {
    public ArteryBlockEntity(BlockPos pos, BlockState state) { super(MeatscapeBlockEntities.ARTERY.get(), pos, state, 500); }
    public static void serverTick(net.minecraft.world.level.Level ignored, BlockPos pos, BlockState state, ArteryBlockEntity artery) {
        if (artery.level instanceof ServerLevel) artery.transferForward(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING));
    }
}
