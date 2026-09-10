package com.bleedthrough.meatscape.world.maw;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Developer-only physical endpoint. Binding is server-authoritative and requires operator permission. */
public final class MawGatewayBlock extends Block {
    public MawGatewayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        MawTransitService.TransitResult result = MawTransitService.useGateway(serverPlayer, serverLevel, pos);
        if (result != MawTransitService.TransitResult.SUCCESS) {
            serverPlayer.displayClientMessage(result.message(), true);
        }
        return InteractionResult.CONSUME;
    }
}
