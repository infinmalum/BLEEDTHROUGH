package com.bleedthrough.meatscape.safety;

import com.bleedthrough.meatscape.coherence.evolution.EvolutionCandidate;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.coherence.rollback.RestorationSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.coherence.CoherenceTier;

/** The only Phase 4 entry point allowed to mutate terrain for an evolution candidate. */
public final class SafeEvolutionConverter {
    private SafeEvolutionConverter() { }

    public static ConversionDecision apply(ServerLevel level, MeatscapeWorldData world, EvolutionCandidate candidate) {
        BlockPos target = candidate.position().below();
        if (!level.hasChunkAt(target)) return ConversionDecision.SKIP_NOT_REPLACEABLE;
        if (com.bleedthrough.meatscape.coherence.thermal.ThermalRules.frozen(level, target)
                || com.bleedthrough.meatscape.coherence.thermal.ThermalRules.suppressed(level, candidate.chunk().pos())) {
            return ConversionDecision.SKIP_NOT_REPLACEABLE;
        }
        BlockState state = level.getBlockState(target);
        boolean absolute = state.is(MeatscapeBlockTags.ABSOLUTE_PROTECTED) || level.getBlockEntity(target) != null;
        var safety = ChunkSafetyService.get(level, target);
        ConversionDecision decision = SafetyPolicy.decide(absolute,
                world.isProtected(level.dimension().location(), target),
                state.is(MeatscapeBlockTags.NATURAL_REPLACEABLE), safety.trust(), safety.isModified(target));
        if (decision == ConversionDecision.DESTRUCTIVE) {
            BlockState replacement = replacementFor(state, CoherenceTier.from(
                    MawCoherenceService.get(level, candidate.chunk().pos())));
            if (level.setBlock(target, replacement, Block.UPDATE_ALL)) {
                safety.recordRestoration(target, RestorationSource.classify(state));
            }
        } else if (decision == ConversionDecision.ATTACHMENT) {
            placeAttachment(level, target);
        }
        return decision;
    }

    static BlockState replacementFor(BlockState original, CoherenceTier tier) {
        RestorationSource source = RestorationSource.classify(original);
        if (source == RestorationSource.SOIL) {
            return (tier == CoherenceTier.QUIET || tier == CoherenceTier.EMERGING
                    ? MeatscapeBlocks.DERMAL_SOIL : MeatscapeBlocks.NUTRIENT_MOUND).get().defaultBlockState();
        }
        if (source == RestorationSource.WOOD) return MeatscapeBlocks.VASCULAR_MAT.get().defaultBlockState();
        if (source == RestorationSource.ICE) return MeatscapeBlocks.OSSIFIED_STONE.get().defaultBlockState();
        return (tier == CoherenceTier.SATURATED ? MeatscapeBlocks.OSSIFIED_STONE : MeatscapeBlocks.CHANGED_STONE)
                .get().defaultBlockState();
    }

    private static void placeAttachment(ServerLevel level, BlockPos surface) {
        BlockPos[] candidates = { surface.above(), surface.north(), surface.south(), surface.east(), surface.west() };
        for (BlockPos pos : candidates) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).isAir()
                    && !com.bleedthrough.meatscape.coherence.thermal.ThermalRules.frozen(level, pos)
                    && !com.bleedthrough.meatscape.coherence.thermal.ThermalRules.suppressed(level, new net.minecraft.world.level.ChunkPos(pos))) {
                if (level.setBlock(pos, MeatscapeBlocks.DERMAL_FILM.get().defaultBlockState(), Block.UPDATE_ALL)) {
                    ChunkSafetyService.get(level, pos).recordRestoration(pos, RestorationSource.ATTACHMENT);
                }
                return;
            }
        }
    }
}
