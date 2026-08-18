package com.bleedthrough.meatscape.client;

import com.bleedthrough.meatscape.Meatscape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Clears the bounded client read model when leaving a server or world. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, value = Dist.CLIENT)
public final class ClientCoherenceEvents {
    private ClientCoherenceEvents() {
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCoherenceState.clear();
    }
}
