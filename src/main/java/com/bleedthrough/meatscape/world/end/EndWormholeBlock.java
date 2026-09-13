package com.bleedthrough.meatscape.world.end;

import com.bleedthrough.meatscape.progression.KnowledgeObservation;
import com.bleedthrough.meatscape.progression.PlayerKnowledge;
import com.bleedthrough.meatscape.world.maw.MawTransitService;
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

/** Stable natural Outer End entrance; it delegates all transport to the shared Maw service. */
public final class EndWormholeBlock extends Block {
    public EndWormholeBlock(Properties properties) { super(properties); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        MawTransitService.TransitResult result = MawTransitService.useEndWormhole(serverPlayer, serverLevel, pos);
        if (result == MawTransitService.TransitResult.SUCCESS) {
            PlayerKnowledge.observe(serverPlayer, KnowledgeObservation.MAW);
            PlayerKnowledge.observe(serverPlayer, KnowledgeObservation.WORMHOLE);
        } else serverPlayer.displayClientMessage(result.message(), true);
        return InteractionResult.CONSUME;
    }
}
