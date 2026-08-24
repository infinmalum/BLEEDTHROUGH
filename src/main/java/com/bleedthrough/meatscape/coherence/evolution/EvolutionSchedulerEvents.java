package com.bleedthrough.meatscape.coherence.evolution;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import com.bleedthrough.meatscape.coherence.rift.RiftFieldCalculator;
import com.bleedthrough.meatscape.coherence.rift.RiftRecord;
import com.bleedthrough.meatscape.core.config.MeatscapeConfig;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.safety.SafeEvolutionConverter;
import com.bleedthrough.meatscape.coherence.rollback.RollbackScheduler;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Owns scheduler instances for running servers and translates Forge lifecycle events into scalar tasks. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class EvolutionSchedulerEvents {
    private static final Map<MinecraftServer, EvolutionScheduler> SCHEDULERS = new IdentityHashMap<>();
    private static final Map<MinecraftServer, RollbackScheduler> ROLLBACK_SCHEDULERS = new IdentityHashMap<>();

    private EvolutionSchedulerEvents() {
    }

    public static EvolutionScheduler get(MinecraftServer server) {
        return SCHEDULERS.computeIfAbsent(server, ignored -> new EvolutionScheduler(
                MeatscapeConfig.EVOLUTION_GLOBAL_BUDGET.get(),
                MeatscapeConfig.EVOLUTION_PER_RIFT_BUDGET.get()));
    }

    public static RollbackScheduler getRollback(MinecraftServer server) {
        return ROLLBACK_SCHEDULERS.computeIfAbsent(server, ignored -> new RollbackScheduler());
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        rebuild(event.getServer());
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        EvolutionScheduler removed = SCHEDULERS.remove(event.getServer());
        ROLLBACK_SCHEDULERS.remove(event.getServer());
        if (removed != null) {
            removed.clear();
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        MeatscapeWorldData data = MeatscapeWorldData.get(server);
        int globalBudget = MeatscapeConfig.EVOLUTION_GLOBAL_BUDGET.get();
        int forwardBudget = data.rollbackJobs().isEmpty() ? globalBudget : Math.max(1, globalBudget / 2);
        var candidates = get(server).tick(
                data.isPaused(), server.overworld().getGameTime(), environment(server, data), forwardBudget);
        for (EvolutionCandidate candidate : candidates) {
            ServerLevel level = level(server, candidate.chunk().dimension());
            if (level != null) SafeEvolutionConverter.apply(level, data, candidate);
        }
        int remainingBudget = Math.max(0, globalBudget - candidates.size());
        getRollback(server).tick(server, data, remainingBudget);
    }

    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        enqueueChunk(level.getServer(), level.dimension().location(), event.getChunk().getPos());
    }

    @SubscribeEvent
    public static void chunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        get(level.getServer()).removeChunk(
                new DimensionChunkKey(level.dimension().location(), event.getChunk().getPos()));
    }

    public static void rebuild(MinecraftServer server) {
        EvolutionScheduler scheduler = get(server);
        scheduler.clear();
        MeatscapeWorldData data = MeatscapeWorldData.get(server);
        for (RiftRecord rift : data.rifts()) {
            enqueueLoadedChunks(server, scheduler, rift);
        }
    }

    public static void enqueueRift(MinecraftServer server, RiftRecord rift) {
        enqueueLoadedChunks(server, get(server), rift);
    }

    private static void enqueueChunk(MinecraftServer server, ResourceLocation dimension, ChunkPos chunkPos) {
        MeatscapeWorldData data = MeatscapeWorldData.get(server);
        for (RiftRecord rift : data.spatialIndex().at(dimension, chunkPos)) {
            if (RiftFieldCalculator.contribution(rift, chunkPos) > 0) {
                get(server).enqueue(new EvolutionTask(rift.id(), new DimensionChunkKey(dimension, chunkPos)));
            }
        }
    }

    private static void enqueueLoadedChunks(
            MinecraftServer server, EvolutionScheduler scheduler, RiftRecord rift) {
        ServerLevel level = level(server, rift.dimension());
        if (level == null) {
            return;
        }
        EvolutionTaskPlanner.forRift(rift, key -> {
            ChunkPos pos = key.pos();
            return level.hasChunk(pos.x, pos.z);
        }).forEach(scheduler::enqueue);
    }

    private static EvolutionEnvironment environment(MinecraftServer server, MeatscapeWorldData data) {
        return new EvolutionEnvironment() {
            @Override
            public boolean riftExists(UUID riftId) {
                return data.findRift(riftId).isPresent();
            }

            @Override
            public boolean chunkLoaded(DimensionChunkKey chunk) {
                ServerLevel level = level(server, chunk.dimension());
                ChunkPos pos = chunk.pos();
                return level != null && level.hasChunk(pos.x, pos.z);
            }

            @Override
            public int coherence(DimensionChunkKey chunk) {
                ServerLevel level = level(server, chunk.dimension());
                ChunkPos pos = chunk.pos();
                if (level == null || !level.hasChunk(pos.x, pos.z)) {
                    return 0;
                }
                return MawCoherenceService.get(level.getChunk(pos.x, pos.z));
            }

            @Override
            public int surfaceY(DimensionChunkKey chunk, int blockX, int blockZ) {
                ServerLevel level = level(server, chunk.dimension());
                if (level == null) {
                    return 0;
                }
                return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
            }
        };
    }

    private static ServerLevel level(MinecraftServer server, ResourceLocation dimension) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
    }
}
