package com.bleedthrough.meatscape.coherence.evolution;

import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import com.bleedthrough.meatscape.coherence.rift.RiftFieldCalculator;
import com.bleedthrough.meatscape.coherence.rift.RiftRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.level.ChunkPos;

/** Rebuilds transient tasks from persistent Rift data and a caller-provided loaded-chunk view. */
public final class EvolutionTaskPlanner {
    private EvolutionTaskPlanner() {
    }

    public static List<EvolutionTask> forRift(
            RiftRecord rift, Predicate<DimensionChunkKey> isLoaded) {
        List<EvolutionTask> tasks = new ArrayList<>();
        int minX = Math.floorDiv(rift.position().getX() - rift.radius(), 16);
        int maxX = Math.floorDiv(rift.position().getX() + rift.radius(), 16);
        int minZ = Math.floorDiv(rift.position().getZ() - rift.radius(), 16);
        int maxZ = Math.floorDiv(rift.position().getZ() + rift.radius(), 16);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                ChunkPos chunkPos = new ChunkPos(x, z);
                DimensionChunkKey key = new DimensionChunkKey(rift.dimension(), chunkPos);
                if (isLoaded.test(key) && RiftFieldCalculator.contribution(rift, chunkPos) > 0) {
                    tasks.add(new EvolutionTask(rift.id(), key));
                }
            }
        }
        return List.copyOf(tasks);
    }
}
