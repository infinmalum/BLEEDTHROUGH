package com.bleedthrough.meatscape.bioindustry;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class HeartPumpBlockEntity extends HematicNodeBlockEntity {
    public static final int TISSUE_YIELD = 250;
    public HeartPumpBlockEntity(BlockPos pos, BlockState state) { super(MeatscapeBlockEntities.HEART_PUMP.get(), pos, state, 4000); }
    public static void serverTick(net.minecraft.world.level.Level ignored, BlockPos pos, BlockState state, HeartPumpBlockEntity pump) {
        if (pump.level instanceof ServerLevel) pump.transferForward(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING));
    }
}
