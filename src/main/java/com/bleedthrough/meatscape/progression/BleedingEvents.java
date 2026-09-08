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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
        if (event.phase != TickEvent.Phase.END || !MeatscapeConfig.BLEEDING_ENABLED.get()) return;
        var server = event.getServer();
        var level = server.overworld();
        var data = MeatscapeWorldData.get(server);
        if (data.bleedingOrigin().isEmpty()) return;
        var origin = data.bleedingOrigin().orElseThrow();
        // A logout or departure must not silently consume the local first event.
        if (level.players().stream().noneMatch(player -> !player.isSpectator()
                && player.blockPosition().distSqr(origin) <= 512.0D * 512.0D)) return;
        if (!data.tickBleedingDelay() || server.getTickCount() % 20 != 0) return;
        BleedingService.activateReady(data, level.getGameTime()).ifPresent(rift -> {
            EvolutionSchedulerEvents.enqueueRift(server, rift);
            RiftCoreBlock.syncLoadedState(level, rift);
            for (ServerPlayer player : level.players()) {
                if (player.blockPosition().distSqr(origin) > 512.0D * 512.0D) continue;
                player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 60, 40));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("event.meatscape.bleeding")));
            }
            level.playSound(null, origin, SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 0.7F, 0.6F);
            Meatscape.LOGGER.info("The Bleeding activated Rift {} at {}", rift.id(), rift.position());
        });
    }
}
