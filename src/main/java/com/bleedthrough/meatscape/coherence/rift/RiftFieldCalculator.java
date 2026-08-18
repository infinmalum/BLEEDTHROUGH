package com.bleedthrough.meatscape.coherence.rift;

import com.bleedthrough.meatscape.coherence.data.MawCoherenceData;
import java.util.Collection;
import net.minecraft.world.level.ChunkPos;

/** Pure deterministic calculation for the abstract coherence field. */
public final class RiftFieldCalculator {
    private static final double DIFFUSION_RATE = 0.05D;

    private RiftFieldCalculator() {
    }

    public static int contribution(RiftRecord rift, ChunkPos chunkPos) {
        double x = (chunkPos.x << 4) + 8.0D;
        double z = (chunkPos.z << 4) + 8.0D;
        double dx = x - rift.position().getX();
        double dz = z - rift.position().getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > rift.radius()) {
            return 0;
        }
        double falloff = 1.0D - distance / rift.radius();
        return Math.max(1, (int) Math.round(rift.strength() * falloff * DIFFUSION_RATE));
    }

    public static int aggregate(Collection<RiftRecord> rifts, ChunkPos chunkPos) {
        int total = 0;
        for (RiftRecord rift : rifts) {
            total = MawCoherenceData.clamp(total + contribution(rift, chunkPos));
        }
        return total;
    }
}
