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
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty ACTIVE =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("active");
    public static final int RADIUS_BLOCKS = 6 * 16;
    public static final int STRENGTH = 24;

    public RiftCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, true));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    public static void syncLoadedState(ServerLevel level, RiftRecord rift) {
        var chunk = level.getChunkSource().getChunkNow(rift.position().getX() >> 4, rift.position().getZ() >> 4);
        if (chunk == null) return;
        BlockState state = chunk.getBlockState(rift.position());
        if (state.getBlock() instanceof RiftCoreBlock && state.getValue(ACTIVE) != rift.active()) {
            level.setBlock(rift.position(), state.setValue(ACTIVE, rift.active()), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!(level instanceof ServerLevel serverLevel)) return;
        MeatscapeWorldData data = MeatscapeWorldData.get(serverLevel.getServer());
        if (oldState.is(this)) {
            data.rifts().stream().filter(rift -> rift.dimension().equals(serverLevel.dimension().location())
                    && rift.position().equals(pos)).toList().forEach(rift -> {
                RiftRecord updated = rift.withActive(state.getValue(ACTIVE));
                data.addRift(updated);
                EvolutionSchedulerEventsBridge.enqueue(serverLevel, updated);
            });
            return;
        }
        boolean exists = data.rifts().stream().anyMatch(rift -> rift.dimension().equals(serverLevel.dimension().location())
                && rift.position().equals(pos));
        if (!exists) {
            RiftRecord rift = new RiftRecord(UUID.randomUUID(), serverLevel.dimension().location(), pos,
                    RADIUS_BLOCKS, STRENGTH, level.getGameTime(), RiftRecord.PERMANENT, state.getValue(ACTIVE));
            data.addRift(rift);
            EvolutionSchedulerEventsBridge.enqueue(serverLevel, rift);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        // Worldgen's persisted one-shot registration tick; never used for per-block infection.
        onPlace(state, level, pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
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
