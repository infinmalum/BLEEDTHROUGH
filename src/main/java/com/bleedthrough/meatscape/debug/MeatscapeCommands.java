package com.bleedthrough.meatscape.debug;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.coherence.data.MawCoherenceData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
                                                IntegerArgumentType.getInteger(context, "value")))))));
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
}
