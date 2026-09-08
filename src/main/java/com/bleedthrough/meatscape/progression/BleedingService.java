package com.bleedthrough.meatscape.progression;

import com.bleedthrough.meatscape.coherence.rift.RiftRecord;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.world.data.WorldStage;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Server-owned progression decisions, without player, entity or client references. */
public final class BleedingService {
    public static final int SEARCH_RADIUS = 512;
    public static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    public static final ResourceLocation NETHER = ResourceLocation.withDefaultNamespace("the_nether");

    private BleedingService() { }

    public static boolean returned(MeatscapeWorldData data, ResourceLocation from,
            ResourceLocation to, BlockPos origin, int delay) {
        return from.equals(NETHER) && to.equals(OVERWORLD)
                && data.scheduleBleeding(origin, delay);
    }

    /** No terrain placement or forced chunk loading. A missing source leaves the event pending. */
    public static Optional<RiftRecord> activateReady(MeatscapeWorldData data, long gameTime) {
        if (data.isPaused() || data.worldStage() != WorldStage.DORMANT
                || data.bleedingOrigin().isEmpty() || data.bleedingDelay() > 0) return Optional.empty();
        BlockPos origin = data.bleedingOrigin().orElseThrow();
        Optional<RiftRecord> nearest = data.rifts().stream()
                .filter(rift -> !rift.active() && !rift.isExpired(gameTime))
                .filter(rift -> rift.dimension().equals(OVERWORLD))
                .filter(rift -> horizontalDistance(rift.position(), origin) <= (double) SEARCH_RADIUS * SEARCH_RADIUS)
                .min(Comparator.comparingDouble((RiftRecord rift) -> horizontalDistance(rift.position(), origin))
                        .thenComparing(RiftRecord::id));
        return nearest.map(rift -> {
            RiftRecord active = rift.withActive(true);
            data.addRift(active);
            data.setWorldStage(WorldStage.BLEEDING);
            return active;
        });
    }

    private static double horizontalDistance(BlockPos a, BlockPos b) {
        double dx = (double) a.getX() - b.getX();
        double dz = (double) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
