package com.bleedthrough.meatscape.coherence.evolution;

import java.util.Map;

public record EvolutionStats(
        long ticks,
        long totalProcessed,
        int lastTickProcessed,
        int queueLength,
        long lastTickNanos,
        Map<EvolutionSkipReason, Long> skipped) {
    public EvolutionStats {
        skipped = Map.copyOf(skipped);
    }
}
