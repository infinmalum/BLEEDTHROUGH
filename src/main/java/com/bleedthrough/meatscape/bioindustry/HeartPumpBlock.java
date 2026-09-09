package com.bleedthrough.meatscape.bioindustry;

import com.bleedthrough.meatscape.core.registry.MeatscapeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Minimal hand-fed processor: two tissue become one collagen without external machinery. */
public final class HeartPumpBlock extends Block {
    public HeartPumpBlock(Properties properties) {
        super(properties);
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
            held.shrink(cost);
            ItemStack result = new ItemStack(MeatscapeItems.COLLAGEN.get());
            if (!player.getInventory().add(result)) player.drop(result, false);
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.swing(hand, true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
