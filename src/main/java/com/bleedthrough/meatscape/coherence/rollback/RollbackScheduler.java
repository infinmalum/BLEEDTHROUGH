package com.bleedthrough.meatscape.coherence.rollback;

import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Consumes only the tick budget left by forward evolution; jobs themselves live in SavedData. */
public final class RollbackScheduler {
    private RollbackStats stats = RollbackStats.EMPTY;

    public RollbackStats tick(MinecraftServer server, MeatscapeWorldData data, int availableBudget) {
        long started = System.nanoTime();
        if (data.isPaused() || availableBudget <= 0 || data.rollbackJobs().isEmpty()) {
            stats = new RollbackStats(0, 0, data.rollbackJobs().size(), false, System.nanoTime() - started);
            return stats;
        }
        int processed = 0;
        int restored = 0;
        boolean waiting = false;
        for (RollbackJob job : List.copyOf(data.rollbackJobs())) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, job.dimension()));
            if (level == null) continue;
            int jobProcessed = 0;
            while (!job.complete() && jobProcessed < job.rate() && processed < availableBudget) {
                var pos = job.currentPosition();
                if (!level.hasChunkAt(pos)) {
                    waiting = true;
                    break;
                }
                RollbackResult result = RollbackService.inspectOrRestore(level, pos, job.dryRun());
                job.advance(result.restorable());
                if (result.restorable()) restored++;
                processed++;
                jobProcessed++;
            }
            if (job.complete()) data.removeRollbackJob(job.id());
            if (processed >= availableBudget) break;
        }
        if (processed > 0) data.rollbackProgressed();
        stats = new RollbackStats(processed, restored, data.rollbackJobs().size(), waiting, System.nanoTime() - started);
        return stats;
    }

    public RollbackStats stats() { return stats; }
}
