package com.bleedthrough.meatscape.coherence;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.core.network.MeatscapeNetwork;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Sends the current authoritative value whenever a player starts watching a chunk. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class CoherenceSyncEvents {
    private CoherenceSyncEvents() {
    }

    @SubscribeEvent
    public static void onWatch(ChunkWatchEvent.Watch event) {
        int value = MawCoherenceService.get(event.getChunk());
        MeatscapeNetwork.sendTo(
                event.getPlayer(),
                MawCoherenceService.payload(event.getLevel(), event.getPos(), value));
    }
}
