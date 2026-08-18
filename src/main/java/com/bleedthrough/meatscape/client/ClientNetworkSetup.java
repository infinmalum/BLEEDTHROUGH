package com.bleedthrough.meatscape.client;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.core.network.MeatscapeNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Installs client packet receivers without exposing client classes to common code. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientNetworkSetup {
    private ClientNetworkSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MeatscapeNetwork.registerClientReceiver(ClientCoherenceState::accept);
    }
}
