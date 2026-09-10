package com.bleedthrough.meatscape.debug;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.evolution.EvolutionSchedulerEvents;
import com.bleedthrough.meatscape.core.config.MeatscapeConfig;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Writes opt-in, bounded server performance samples for real-time soak validation. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class SoakTelemetry {
    static final String HEADER = "timestamp,server_tick,mspt,heap_used_bytes,heap_max_bytes,world_bytes,"
            + "rift_count,queue_length,processed,last_tick_nanos,rollback_jobs";
    private static final Map<MinecraftServer, Long> NEXT_SAMPLE = new IdentityHashMap<>();

    private SoakTelemetry() {
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        if (!MeatscapeConfig.SOAK_TELEMETRY_ENABLED.get()) {
            return;
        }
        MinecraftServer server = event.getServer();
        NEXT_SAMPLE.put(server, server.overworld().getGameTime());
        appendHeader(server);
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        NEXT_SAMPLE.remove(event.getServer());
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !MeatscapeConfig.SOAK_TELEMETRY_ENABLED.get()) {
            return;
        }
        MinecraftServer server = event.getServer();
        long gameTime = server.overworld().getGameTime();
        long next = NEXT_SAMPLE.getOrDefault(server, gameTime);
        if (gameTime < next) {
            return;
        }
        NEXT_SAMPLE.put(server, gameTime + MeatscapeConfig.SOAK_TELEMETRY_INTERVAL_TICKS.get());
        appendSample(server, sample(server));
    }

    static Sample sample(MinecraftServer server) {
        var evolution = EvolutionSchedulerEvents.get(server).stats();
        Runtime runtime = Runtime.getRuntime();
        return new Sample(
                Instant.now().toString(),
                server.getTickCount(),
                server.getAverageTickTime(),
                runtime.totalMemory() - runtime.freeMemory(),
                runtime.maxMemory(),
                directorySize(server.getWorldPath(LevelResource.ROOT)),
                MeatscapeWorldData.get(server).rifts().size(),
                evolution.queueLength(),
                evolution.lastTickProcessed(),
                evolution.lastTickNanos(),
                MeatscapeWorldData.get(server).rollbackJobs().size());
    }

    private static void appendHeader(MinecraftServer server) {
        Path path = outputPath(server);
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path) || Files.size(path) == 0L) {
                Files.writeString(path, HEADER + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException exception) {
            Meatscape.LOGGER.warn("Unable to initialize soak telemetry at {}", path, exception);
        }
    }

    private static void appendSample(MinecraftServer server, Sample sample) {
        Path path = outputPath(server);
        try {
            Files.writeString(path, sample.csv() + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            Meatscape.LOGGER.warn("Unable to append soak telemetry at {}", path, exception);
        }
    }

    private static Path outputPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("meatscape-soak.csv");
    }

    static long directorySize(Path root) {
        if (!Files.exists(root)) {
            return 0L;
        }
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).mapToLong(SoakTelemetry::size).sum();
        } catch (IOException exception) {
            Meatscape.LOGGER.warn("Unable to measure soak world size at {}", root, exception);
            return -1L;
        }
    }

    private static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            return 0L;
        }
    }

    record Sample(String timestamp, int serverTick, float mspt, long heapUsedBytes, long heapMaxBytes,
                  long worldBytes, int riftCount, int queueLength, int processed, long lastTickNanos,
                  int rollbackJobs) {
        String csv() {
            return timestamp + ',' + serverTick + ',' + mspt + ',' + heapUsedBytes + ',' + heapMaxBytes + ','
                    + worldBytes + ',' + riftCount + ',' + queueLength + ',' + processed + ',' + lastTickNanos
                    + ',' + rollbackJobs;
        }
    }
}
