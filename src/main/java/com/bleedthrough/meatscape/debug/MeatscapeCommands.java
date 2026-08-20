package com.bleedthrough.meatscape.debug;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.coherence.evolution.EvolutionSchedulerEvents;
import com.bleedthrough.meatscape.coherence.rift.RiftRecord;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.safety.ChunkSafetyService;
import com.bleedthrough.meatscape.safety.ProtectedRegion;
import com.bleedthrough.meatscape.safety.TerrainTrust;
import com.bleedthrough.meatscape.coherence.rollback.RollbackJob;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Development commands for inspecting the authoritative coherence state. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class MeatscapeCommands {
    private MeatscapeCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("meatscape")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("coherence")
                        .then(Commands.literal("get")
                                .executes(context -> get(context.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                        .executes(context -> set(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "value"))))))
                .then(Commands.literal("rift")
                        .then(Commands.literal("create")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(
                                                RiftRecord.MIN_RADIUS, RiftRecord.MAX_RADIUS))
                                        .then(Commands.argument("strength", IntegerArgumentType.integer(
                                                        RiftRecord.MIN_STRENGTH, RiftRecord.MAX_STRENGTH))
                                                .executes(context -> createRift(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "radius"),
                                                        IntegerArgumentType.getInteger(context, "strength"),
                                                        RiftRecord.PERMANENT))
                                                .then(Commands.argument("lifetimeTicks", LongArgumentType.longArg(1L))
                                                        .executes(context -> createRift(
                                                                context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "radius"),
                                                                IntegerArgumentType.getInteger(context, "strength"),
                                                                LongArgumentType.getLong(context, "lifetimeTicks")))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> removeRift(
                                                context.getSource(), StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> inspectRift(
                                                context.getSource(), StringArgumentType.getString(context, "id"))))))
                .then(Commands.literal("pause")
                        .executes(context -> inspectPause(context.getSource()))
                        .then(Commands.argument("paused", BoolArgumentType.bool())
                                .executes(context -> setPause(
                                        context.getSource(), BoolArgumentType.getBool(context, "paused")))))
                .then(Commands.literal("trust")
                        .executes(context -> setTrust(context.getSource(), TerrainTrust.TRUSTED)))
                .then(Commands.literal("untrust")
                        .executes(context -> setTrust(context.getSource(), TerrainTrust.UNTRUSTED)))
                .then(Commands.literal("protect")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 512))
                                .executes(context -> protect(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "radius")))))
                .then(Commands.literal("inspect")
                        .executes(context -> inspectSafety(context.getSource())))
                .then(Commands.literal("rollback")
                        .then(Commands.literal("start")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                                        .then(Commands.argument("rate", IntegerArgumentType.integer(1, 4096))
                                                .executes(context -> startRollback(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "radius"),
                                                        IntegerArgumentType.getInteger(context, "rate"), false))
                                                .then(Commands.argument("dryRun", BoolArgumentType.bool())
                                                        .executes(context -> startRollback(
                                                                context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "radius"),
                                                                IntegerArgumentType.getInteger(context, "rate"),
                                                                BoolArgumentType.getBool(context, "dryRun")))))))
                        .then(Commands.literal("cancel")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> cancelRollback(
                                                context.getSource(), StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("status")
                                .executes(context -> rollbackStatus(context.getSource()))))
                .then(Commands.literal("debug")
                        .then(Commands.literal("stats")
                                .executes(context -> debugStats(context.getSource())))));
    }

    private static int get(CommandSourceStack source) {
        ChunkPos chunkPos = sourceChunk(source);
        int value = MawCoherenceService.get(source.getLevel(), chunkPos);
        source.sendSuccess(() -> Component.literal("Maw Coherence " + chunkPos + " = " + value + "%"), false);
        Meatscape.LOGGER.info("Inspected Maw Coherence dimension={} chunk={} value={}",
                source.getLevel().dimension().location(), chunkPos, value);
        return Command.SINGLE_SUCCESS;
    }

    private static int set(CommandSourceStack source, int requestedValue) {
        ChunkPos chunkPos = sourceChunk(source);
        int value = MawCoherenceService.set(source.getLevel(), chunkPos, requestedValue);
        source.sendSuccess(() -> Component.literal("Maw Coherence " + chunkPos + " set to " + value + "%"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static ChunkPos sourceChunk(CommandSourceStack source) {
        return new ChunkPos(BlockPos.containing(source.getPosition()));
    }

    private static int createRift(CommandSourceStack source, int radius, int strength, long lifetimeTicks) {
        RiftRecord rift = new RiftRecord(
                UUID.randomUUID(),
                source.getLevel().dimension().location(),
                BlockPos.containing(source.getPosition()),
                radius,
                strength,
                source.getLevel().getGameTime(),
                lifetimeTicks);
        MeatscapeWorldData.get(source.getServer()).addRift(rift);
        EvolutionSchedulerEvents.enqueueRift(source.getServer(), rift);
        source.sendSuccess(() -> Component.literal("Created Rift " + rift.id()), true);
        Meatscape.LOGGER.info("Created Rift id={} dimension={} position={} radius={} strength={} lifetime={}",
                rift.id(), rift.dimension(), rift.position(), rift.radius(), rift.strength(), rift.lifetimeTicks());
        return Command.SINGLE_SUCCESS;
    }

    private static int removeRift(CommandSourceStack source, String idText) {
        UUID id = parseUuid(source, idText);
        if (id == null) {
            return 0;
        }
        boolean removed = MeatscapeWorldData.get(source.getServer()).removeRift(id);
        source.sendSuccess(() -> Component.literal(removed ? "Removed Rift " + id : "Rift not found: " + id), true);
        return removed ? Command.SINGLE_SUCCESS : 0;
    }

    private static int inspectRift(CommandSourceStack source, String idText) {
        UUID id = parseUuid(source, idText);
        if (id == null) {
            return 0;
        }
        var rift = MeatscapeWorldData.get(source.getServer()).findRift(id);
        if (rift.isEmpty()) {
            source.sendFailure(Component.literal("Rift not found: " + id));
            return 0;
        }
        RiftRecord value = rift.orElseThrow();
        source.sendSuccess(() -> Component.literal("Rift " + value.id()
                + " dimension=" + value.dimension()
                + " position=" + value.position().toShortString()
                + " radius=" + value.radius()
                + " strength=" + value.strength()
                + " lifetime=" + value.lifetimeTicks()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int inspectPause(CommandSourceStack source) {
        boolean paused = MeatscapeWorldData.get(source.getServer()).isPaused();
        source.sendSuccess(() -> Component.literal("Rift field paused=" + paused), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setPause(CommandSourceStack source, boolean paused) {
        MeatscapeWorldData.get(source.getServer()).setPaused(paused);
        source.sendSuccess(() -> Component.literal("Rift field paused=" + paused), true);
        return Command.SINGLE_SUCCESS;
    }

    private static UUID parseUuid(CommandSourceStack source, String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("Invalid Rift UUID: " + text));
            return null;
        }
    }

    private static int debugStats(CommandSourceStack source) {
        var stats = EvolutionSchedulerEvents.get(source.getServer()).stats();
        var rollback = EvolutionSchedulerEvents.getRollback(source.getServer()).stats();
        double millis = stats.lastTickNanos() / 1_000_000.0D;
        source.sendSuccess(() -> Component.literal("Evolution Scheduler: processed="
                + stats.lastTickProcessed()
                + " total=" + stats.totalProcessed()
                + " queue=" + stats.queueLength()
                + " tickMs=" + String.format(java.util.Locale.ROOT, "%.3f", millis)
                + " skipped=" + stats.skipped()
                + " rollbackProcessed=" + rollback.processed()
                + " rollbackRestored=" + rollback.restored()
                + " rollbackJobs=" + rollback.activeJobs()
                + " rollbackWaiting=" + rollback.waitingForChunk()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setTrust(CommandSourceStack source, TerrainTrust trust) {
        ChunkPos pos = sourceChunk(source);
        ChunkSafetyService.setTrust(source.getLevel(), pos, trust);
        source.sendSuccess(() -> Component.literal("Terrain trust " + pos + " set to " + trust), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int protect(CommandSourceStack source, int radius) {
        BlockPos center = BlockPos.containing(source.getPosition());
        ProtectedRegion region = new ProtectedRegion(UUID.randomUUID(), source.getLevel().dimension().location(),
                center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius), null);
        MeatscapeWorldData.get(source.getServer()).addProtectedRegion(region);
        source.sendSuccess(() -> Component.literal("Protected region " + region.id() + " radius=" + radius), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int inspectSafety(CommandSourceStack source) {
        BlockPos pos = BlockPos.containing(source.getPosition());
        var data = ChunkSafetyService.get(source.getLevel(), pos);
        boolean protectedHere = MeatscapeWorldData.get(source.getServer())
                .isProtected(source.getLevel().dimension().location(), pos);
        source.sendSuccess(() -> Component.literal("Safety: trust=" + data.trust()
                + " modifiedPositions=" + data.modifiedCount()
                + " positionModified=" + data.isModified(pos)
                + " protected=" + protectedHere), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int startRollback(CommandSourceStack source, int radius, int rate, boolean dryRun) {
        BlockPos center = BlockPos.containing(source.getPosition());
        RollbackJob job = new RollbackJob(UUID.randomUUID(), source.getLevel().dimension().location(),
                center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius), rate, dryRun);
        MeatscapeWorldData.get(source.getServer()).addRollbackJob(job);
        source.sendSuccess(() -> Component.literal("Started rollback " + job.id()
                + " volume=" + job.volume() + " rate=" + job.rate() + " dryRun=" + job.dryRun()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int cancelRollback(CommandSourceStack source, String idText) {
        UUID id = parseUuid(source, idText);
        if (id == null) return 0;
        boolean removed = MeatscapeWorldData.get(source.getServer()).removeRollbackJob(id);
        source.sendSuccess(() -> Component.literal(removed ? "Cancelled rollback " + id : "Rollback not found: " + id), true);
        return removed ? Command.SINGLE_SUCCESS : 0;
    }

    private static int rollbackStatus(CommandSourceStack source) {
        var jobs = MeatscapeWorldData.get(source.getServer()).rollbackJobs();
        if (jobs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No active rollback jobs"), false);
            return Command.SINGLE_SUCCESS;
        }
        for (RollbackJob job : jobs) {
            source.sendSuccess(() -> Component.literal("Rollback " + job.id()
                    + " cursor=" + job.cursor() + "/" + job.volume()
                    + " restorable=" + job.restored() + " skipped=" + job.skipped()
                    + " rate=" + job.rate() + " dryRun=" + job.dryRun()), false);
        }
        return jobs.size();
    }
}
