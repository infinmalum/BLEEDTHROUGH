package com.bleedthrough.meatscape.coherence.evolution;

import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/** Diagnostic-only position that would be processed by a future evolution pass. */
public record EvolutionCandidate(UUID riftId, DimensionChunkKey chunk, BlockPos position) {
}
