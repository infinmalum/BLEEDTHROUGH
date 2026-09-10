package com.bleedthrough.meatscape.world.maw;

import com.bleedthrough.meatscape.Meatscape;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Clears scalar cooldown state promptly; portal tickets themselves are scoped with try/finally per crossing. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class MawTransitEvents {
    private MawTransitEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MawTransitService.forgetPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MawTransitService.clearTransientState();
    }
}
