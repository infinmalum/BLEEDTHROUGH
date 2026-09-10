package com.bleedthrough.meatscape.coherence.thermal;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.coherence.rollback.RollbackResult;
import com.bleedthrough.meatscape.coherence.rollback.RollbackService;
import com.bleedthrough.meatscape.coherence.rollback.RestorationSource;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.safety.ChunkSafetyService;
import com.bleedthrough.meatscape.safety.MeatscapeBlockTags;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.progression.KnowledgeObservation;
import com.bleedthrough.meatscape.progression.PlayerKnowledge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Deliberate one-block experiment, not ambient fire spread or a world rollback. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class Cauterization {
    private Cauterization() { }

    public static boolean apply(ServerLevel level, BlockPos pos) {
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return false;
        var world = MeatscapeWorldData.get(level.getServer());
        if (world.isPaused()) return false;
        var state = level.getBlockState(pos);
        var safety = ChunkSafetyService.get(level, pos);
        boolean film = safety.restorationSource(pos) == RestorationSource.ATTACHMENT;
        if (!state.is(MeatscapeBlockTags.CAUTERIZABLE)
                || state.is(MeatscapeBlockTags.ABSOLUTE_PROTECTED) || state.is(MeatscapeBlockTags.ROLLBACK_PERMANENT)
                || level.getBlockEntity(pos) != null || safety.isModified(pos)
                || (world.isProtected(level.dimension().location(), pos) && !film)
                || RollbackService.inspectOrRestore(level, pos, true) != RollbackResult.WOULD_RESTORE) return false;
        if (!level.setBlock(pos, (film ? Blocks.AIR : MeatscapeBlocks.CHARRED_SCAR.get()).defaultBlockState(), Block.UPDATE_ALL)) return false;
        safety.clearRestoration(pos);
        ChunkPos chunk = new ChunkPos(pos);
        world.suppress(ThermalRules.key(level, chunk));
        MawCoherenceService.set(level, chunk, MawCoherenceService.get(level, chunk) - 10);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 0.7F);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var held = event.getItemStack();
        if (!player.isShiftKeyDown() || !held.is(Items.FLINT_AND_STEEL)) return;
        if (event.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.DENY
                || event.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.DENY) return;
        // Reserve crouch + flint-and-steel on biological terrain so a rejected experiment
        // cannot fall through and accidentally ignite a protected build.
        if (!event.getLevel().getBlockState(event.getPos()).is(MeatscapeBlockTags.CAUTERIZABLE)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (player.isSpectator() || !player.getAbilities().mayBuild || !level.mayInteract(player, event.getPos())) return;
        if (apply(level, event.getPos())) {
            held.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(event.getHand()));
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                PlayerKnowledge.observe(serverPlayer, KnowledgeObservation.CAUTERIZATION);
            }
            player.displayClientMessage(Component.translatable("message.meatscape.cauterized"), true);
        } else player.displayClientMessage(Component.translatable("message.meatscape.cauterize_refused"), true);
    }
}
