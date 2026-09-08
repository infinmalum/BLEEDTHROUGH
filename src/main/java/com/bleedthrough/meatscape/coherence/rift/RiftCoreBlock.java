package com.bleedthrough.meatscape.coherence.rift;

import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A visible, persistent Rift whose block lifetime owns an abstract diffusible source. */
public final class RiftCoreBlock extends Block {
    public static final int RADIUS_CHUNKS = 6;
    public static final int STRENGTH = 24;

    public RiftCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!(level instanceof ServerLevel serverLevel) || oldState.is(this)) return;
        MeatscapeWorldData data = MeatscapeWorldData.get(serverLevel.getServer());
        boolean exists = data.rifts().stream().anyMatch(rift -> rift.dimension().equals(serverLevel.dimension().location())
                && rift.position().equals(pos));
        if (!exists) {
            RiftRecord rift = new RiftRecord(UUID.randomUUID(), serverLevel.dimension().location(), pos,
                    RADIUS_CHUNKS, STRENGTH, level.getGameTime(), RiftRecord.PERMANENT);
            data.addRift(rift);
            EvolutionSchedulerEventsBridge.enqueue(serverLevel, rift);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel && !newState.is(this)) {
            MeatscapeWorldData data = MeatscapeWorldData.get(serverLevel.getServer());
            data.rifts().stream().filter(rift -> rift.dimension().equals(serverLevel.dimension().location())
                    && rift.position().equals(pos)).map(RiftRecord::id).toList().forEach(data::removeRift);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static final class EvolutionSchedulerEventsBridge {
        static void enqueue(ServerLevel level, RiftRecord rift) {
            com.bleedthrough.meatscape.coherence.evolution.EvolutionSchedulerEvents.enqueueRift(level.getServer(), rift);
        }
    }
}
