package com.bleedthrough.meatscape.coherence.evolution;

import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import java.util.Objects;
import java.util.UUID;

/** Rebuildable scalar task key; deliberately contains no chunk, level, or entity reference. */
public record EvolutionTask(UUID riftId, DimensionChunkKey chunk) {
    public EvolutionTask {
        Objects.requireNonNull(riftId, "riftId");
        Objects.requireNonNull(chunk, "chunk");
    }
}
