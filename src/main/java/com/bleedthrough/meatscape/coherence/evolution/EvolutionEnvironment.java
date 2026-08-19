package com.bleedthrough.meatscape.coherence.evolution;

import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import java.util.UUID;

/** Per-tick server view passed into the scheduler and never retained by it. */
public interface EvolutionEnvironment {
    boolean riftExists(UUID riftId);

    boolean chunkLoaded(DimensionChunkKey chunk);

    int coherence(DimensionChunkKey chunk);

    int surfaceY(DimensionChunkKey chunk, int blockX, int blockZ);
}
