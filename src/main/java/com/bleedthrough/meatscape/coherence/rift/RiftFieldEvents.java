package com.bleedthrough.meatscape.coherence.rift;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.coherence.thermal.ThermalRules;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-authoritative Rift lifecycle and abstract field updates. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class RiftFieldEvents {
    public static final int UPDATE_INTERVAL_TICKS = 20;

    private RiftFieldEvents() {
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % UPDATE_INTERVAL_TICKS == 0) {
            update(event.getServer());
        }
    }

    public static void update(MinecraftServer server) {
        MeatscapeWorldData data = MeatscapeWorldData.get(server);
        if (data.isPaused()) {
            return;
        }

        Map<DimensionChunkKey, Integer> contributions = new HashMap<>();
        for (RiftRecord rift : data.rifts()) {
            ServerLevel level = level(server, rift);
            if (level == null) {
                continue;
            }
            if (rift.isExpired(level.getGameTime())) {
                data.removeRift(rift.id());
                continue;
            }
            RiftCoreBlock.syncLoadedState(level, rift);
            if (rift.active()) accumulate(rift, contributions);
        }

        contributions.forEach((key, delta) -> apply(server, data, key, delta));
    }

    private static void accumulate(RiftRecord rift, Map<DimensionChunkKey, Integer> contributions) {
        int minX = Math.floorDiv(rift.position().getX() - rift.radius(), 16);
        int maxX = Math.floorDiv(rift.position().getX() + rift.radius(), 16);
        int minZ = Math.floorDiv(rift.position().getZ() - rift.radius(), 16);
        int maxZ = Math.floorDiv(rift.position().getZ() + rift.radius(), 16);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                ChunkPos chunkPos = new ChunkPos(x, z);
                int delta = RiftFieldCalculator.contribution(rift, chunkPos);
                if (delta > 0) {
                    contributions.merge(
                            new DimensionChunkKey(rift.dimension(), chunkPos), delta, Integer::sum);
                }
            }
        }
    }

    private static void apply(
            MinecraftServer server, MeatscapeWorldData data, DimensionChunkKey key, int requestedDelta) {
        ServerLevel level = level(server, key.dimension());
        if (level == null) {
            return;
        }
        int delta = Math.min(100, requestedDelta);
        ChunkPos pos = key.pos();
        int cap = ThermalRules.cap(level, pos);
        data.capPendingCoherence(key, cap);
        if (ThermalRules.suppressed(level, pos)) delta = 0;
        // A fully frozen chunk gains at most one point per ten field updates.
        // Use the same server-tick clock as the update trigger. Saved world time can
        // have a different modulo-20 offset after restart and must not stall forever.
        if (cap == 15) delta = server.getTickCount() % 200 == 0 ? Math.min(delta, 1) : 0;
        LevelChunk loaded = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (loaded != null) {
            MawCoherenceService.addFromRift(level, loaded, delta);
        } else {
            data.addPendingCoherence(key, delta);
            data.capPendingCoherence(key, cap);
        }
    }

    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        MeatscapeWorldData data = MeatscapeWorldData.get(level.getServer());
        DimensionChunkKey key = new DimensionChunkKey(level.dimension().location(), chunk.getPos());
        int pending = data.consumePendingCoherence(key);
        // Use the callback's chunk directly; querying its FULL future here can deadlock.
        MawCoherenceService.addFromRift(level, chunk, pending);
    }

    private static ServerLevel level(MinecraftServer server, RiftRecord rift) {
        return level(server, rift.dimension());
    }

    private static ServerLevel level(MinecraftServer server, net.minecraft.resources.ResourceLocation dimension) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
    }
}
