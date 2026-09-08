package com.bleedthrough.meatscape.core.network;

import com.bleedthrough.meatscape.Meatscape;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Single protocol channel for bounded Meatscape client synchronization. */
public final class MeatscapeNetwork {
    private static final String PROTOCOL = "2";
    private static volatile Consumer<PreludePayload> preludeReceiver = payload -> { };
    private static volatile Consumer<CoherenceSyncPayload> clientReceiver = payload -> { };
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private MeatscapeNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(PreludePayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PreludePayload::encode).decoder(PreludePayload::decode)
                .consumerMainThread((payload, context) -> {
                    preludeReceiver.accept(payload);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(CoherenceSyncPayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CoherenceSyncPayload::encode)
                .decoder(CoherenceSyncPayload::decode)
                .consumerMainThread(MeatscapeNetwork::handleCoherence)
                .add();
    }

    private static void handleCoherence(CoherenceSyncPayload payload, Supplier<NetworkEvent.Context> context) {
        clientReceiver.accept(payload);
        context.get().setPacketHandled(true);
    }

    public static void registerClientReceiver(Consumer<CoherenceSyncPayload> receiver) {
        clientReceiver = receiver;
    }

    public static void registerPreludeReceiver(Consumer<PreludePayload> receiver) { preludeReceiver = receiver; }

    public static void sendPrelude(ServerPlayer player, int elapsed) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PreludePayload(elapsed));
    }

    public static void sendTo(ServerPlayer player, CoherenceSyncPayload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public static void sendToTracking(LevelChunk chunk, CoherenceSyncPayload payload) {
        CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), payload);
    }
}
