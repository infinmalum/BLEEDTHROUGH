package com.bleedthrough.meatscape.world.data;

import com.bleedthrough.meatscape.Meatscape;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Ensures the versioned world record exists from the first successful server start. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class WorldDataEvents {
    private WorldDataEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MeatscapeWorldData data = MeatscapeWorldData.get(event.getServer());
        Meatscape.LOGGER.info("Loaded Meatscape world data schema={} stage={}",
                data.schemaVersion(), data.worldStage());
    }
}
