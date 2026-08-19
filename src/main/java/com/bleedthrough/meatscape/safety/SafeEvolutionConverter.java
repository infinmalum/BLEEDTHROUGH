package com.bleedthrough.meatscape.safety;

import com.bleedthrough.meatscape.coherence.evolution.EvolutionCandidate;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The only Phase 4 entry point allowed to mutate terrain for an evolution candidate. */
public final class SafeEvolutionConverter {
    private SafeEvolutionConverter() { }

    public static ConversionDecision apply(ServerLevel level, MeatscapeWorldData world, EvolutionCandidate candidate) {
        BlockPos target = candidate.position().below();
        if (!level.hasChunkAt(target)) return ConversionDecision.SKIP_NOT_REPLACEABLE;
        BlockState state = level.getBlockState(target);
        boolean absolute = state.is(MeatscapeBlockTags.ABSOLUTE_PROTECTED) || level.getBlockEntity(target) != null;
        var safety = ChunkSafetyService.get(level, target);
        ConversionDecision decision = SafetyPolicy.decide(absolute,
                world.isProtected(level.dimension().location(), target),
                state.is(MeatscapeBlockTags.NATURAL_REPLACEABLE), safety.trust(), safety.isModified(target));
        if (decision == ConversionDecision.DESTRUCTIVE) {
            level.setBlock(target, MeatscapeBlocks.CHANGED_STONE.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (decision == ConversionDecision.ATTACHMENT) {
            placeAttachment(level, target);
        }
        return decision;
    }

    private static void placeAttachment(ServerLevel level, BlockPos surface) {
        BlockPos[] candidates = { surface.above(), surface.north(), surface.south(), surface.east(), surface.west() };
        for (BlockPos pos : candidates) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).isAir()) {
                level.setBlock(pos, MeatscapeBlocks.DERMAL_FILM.get().defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
        }
    }
}
