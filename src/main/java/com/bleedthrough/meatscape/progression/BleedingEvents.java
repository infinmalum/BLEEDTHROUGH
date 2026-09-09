package com.bleedthrough.meatscape.progression;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.evolution.EvolutionSchedulerEvents;
import com.bleedthrough.meatscape.coherence.rift.RiftCoreBlock;
import com.bleedthrough.meatscape.core.config.MeatscapeConfig;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import com.bleedthrough.meatscape.core.network.MeatscapeNetwork;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class BleedingEvents {
    private BleedingEvents() { }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator()
                && MeatscapeConfig.BLEEDING_ENABLED.get()) {
            BleedingService.returned(MeatscapeWorldData.get(player.getServer()), event.getFrom().location(), event.getTo().location(),
                    player.blockPosition(), MeatscapeConfig.BLEEDING_DELAY.get());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer();
        advance(server);
        if (server.getTickCount() % 5 == 0) {
            var data = MeatscapeWorldData.get(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                boolean visible = !player.isSpectator() && MeatscapeConfig.BLEEDING_ENABLED.get() && !data.isPaused()
                        && player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                        && data.bleedingOrigin().filter(origin -> player.blockPosition().distSqr(origin) <= 128.0D * 128.0D).isPresent();
                MeatscapeNetwork.sendPrelude(player, visible ? data.preludeElapsed() : -1);
            }
        }
    }

    private static void advance(net.minecraft.server.MinecraftServer server) {
        if (!MeatscapeConfig.BLEEDING_ENABLED.get()) return;
        var level = server.overworld();
        var data = MeatscapeWorldData.get(server);
        if (data.isPaused() || data.bleedingOrigin().isEmpty()) return;
        // No retro-generation: a waiting event can follow exploration to a naturally existing source.
        if (data.preludeElapsed() < 0 && server.getTickCount() % 20 == 0
                && BleedingService.nearestDormant(data, data.bleedingOrigin().orElseThrow(), level.getGameTime()).isEmpty()) {
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && BleedingService.nearestDormant(data, player.blockPosition(), level.getGameTime()).isPresent()) {
                    data.relocateBleeding(player.blockPosition());
                    break;
                }
            }
        }
        var origin = data.bleedingOrigin().orElseThrow();
        // A logout or departure must not silently consume the local first event.
        if (level.players().stream().noneMatch(player -> !player.isSpectator()
                && player.blockPosition().distSqr(origin) <= 128.0D * 128.0D)) return;
        if (!data.tickBleedingDelay()) return;
        if ((data.preludeElapsed() < 0 || data.preludeElapsed() >= PreludeTimeline.DURATION)
                && server.getTickCount() % 20 != 0) return;
        BleedingService.advancePrelude(data, level.getGameTime()).ifPresent(rift -> {
            EvolutionSchedulerEvents.enqueueRift(server, rift);
            RiftCoreBlock.syncLoadedState(level, rift);
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || player.blockPosition().distSqr(origin) > 128.0D * 128.0D) continue;
                player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 60, 40));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("event.meatscape.bleeding")));
            }
            Meatscape.LOGGER.info("The Bleeding activated Rift {} at {}", rift.id(), rift.position());
        });
    }
}
