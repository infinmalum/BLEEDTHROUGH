package com.bleedthrough.meatscape.bioindustry;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class HematicActuatorBlockEntity extends HematicNodeBlockEntity {
    public static final int CONSUMPTION_PER_TICK = 10;
    public HematicActuatorBlockEntity(BlockPos pos, BlockState state) { super(MeatscapeBlockEntities.HEMATIC_ACTUATOR.get(), pos, state, 500); }
    public static void serverTick(net.minecraft.world.level.Level ignored, BlockPos pos, BlockState state, HematicActuatorBlockEntity actuator) {
        if (!(actuator.level instanceof ServerLevel level)) return;
        boolean powered = actuator.hematic.drain(CONSUMPTION_PER_TICK) == CONSUMPTION_PER_TICK;
        if (powered) actuator.setChanged();
        if (state.getValue(HematicActuatorBlock.POWERED) != powered) level.setBlock(pos, state.setValue(HematicActuatorBlock.POWERED, powered), 3);
    }
}
