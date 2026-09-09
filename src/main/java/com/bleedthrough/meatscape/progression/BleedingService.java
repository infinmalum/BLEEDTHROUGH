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
        Optional<RiftRecord> nearest = nearestDormant(data, data.bleedingOrigin().orElseThrow(), gameTime);
        return nearest.map(rift -> {
            RiftRecord active = rift.withActive(true);
            data.addRift(active);
            data.setWorldStage(WorldStage.BLEEDING);
            return active;
        });
    }

    public static Optional<RiftRecord> nearestDormant(MeatscapeWorldData data, BlockPos origin, long gameTime) {
        return data.rifts().stream()
                .filter(rift -> !rift.active() && !rift.isExpired(gameTime))
                .filter(rift -> rift.dimension().equals(OVERWORLD))
                .filter(rift -> horizontalDistance(rift.position(), origin) <= (double) SEARCH_RADIUS * SEARCH_RADIUS)
                .min(Comparator.comparingDouble((RiftRecord rift) -> horizontalDistance(rift.position(), origin))
                        .thenComparing(RiftRecord::id));
    }

    /** Advances only while the caller has a local observer. The persisted timer never replays on reload. */
    public static Optional<RiftRecord> advancePrelude(MeatscapeWorldData data, long gameTime) {
        if (data.isPaused() || data.bleedingOrigin().isEmpty() || data.bleedingDelay() > 0) return Optional.empty();
        if (data.preludeElapsed() < 0) {
            if (nearestDormant(data, data.bleedingOrigin().orElseThrow(), gameTime).isPresent()) data.setPreludeElapsed(0);
            return Optional.empty();
        }
        if (data.preludeElapsed() < PreludeTimeline.DURATION) {
            data.setPreludeElapsed(data.preludeElapsed() + 1);
            return Optional.empty();
        }
        var activated = activateReady(data, gameTime);
        if (activated.isEmpty()) data.setPreludeElapsed(-1); // Source removed: permit exploration again.
        return activated;
    }

    private static double horizontalDistance(BlockPos a, BlockPos b) {
        double dx = (double) a.getX() - b.getX();
        double dz = (double) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
