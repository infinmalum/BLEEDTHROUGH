package com.bleedthrough.meatscape.architecture;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import com.bleedthrough.meatscape.core.registry.MeatscapeItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** A player-built wall that survives explosions as a wound and consumes stored nutrition to heal. */
public final class RegenerativeMembraneBlock extends BaseEntityBlock {
    public static final BooleanProperty WOUNDED = BooleanProperty.create("wounded");

    public RegenerativeMembraneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WOUNDED, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(WOUNDED);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override @Nullable public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RegenerativeMembraneBlockEntity(pos, state);
    }

    @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, MeatscapeBlockEntities.REGENERATIVE_MEMBRANE.get(),
                RegenerativeMembraneBlockEntity::serverTick);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(MeatscapeItemTags.NUTRIENT_PASTES)) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RegenerativeMembraneBlockEntity membrane) {
            if (!membrane.addNutrition()) {
                player.displayClientMessage(Component.translatable("message.meatscape.membrane_full"), true);
                return InteractionResult.CONSUME;
            }
            if (!player.getAbilities().instabuild) held.shrink(1);
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.swing(hand, true);
            player.displayClientMessage(Component.translatable("message.meatscape.membrane_fed", membrane.nutrition()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (state.getValue(WOUNDED)) {
            super.onBlockExploded(state, level, pos, explosion); // An unrepaired wall is not an immortal blast shield.
        } else if (!level.isClientSide) level.setBlock(pos, state.setValue(WOUNDED, true), UPDATE_ALL);
    }

    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RegenerativeMembraneBlockEntity membrane
                ? membrane.nutrition() * 15 / RegenerativeMembraneBlockEntity.MAX_NUTRITION : 0;
    }
}
