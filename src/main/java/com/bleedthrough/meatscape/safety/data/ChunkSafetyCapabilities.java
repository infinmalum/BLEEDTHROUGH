package com.bleedthrough.meatscape.safety.data;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.safety.TerrainTrust;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ChunkSafetyCapabilities {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "chunk_safety");
    private ChunkSafetyCapabilities() { }

    @Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterCapabilitiesEvent event) {
            event.register(ChunkSafetyData.class);
        }
    }

    @Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
    public static final class Events {
        @SubscribeEvent public static void attach(AttachCapabilitiesEvent<LevelChunk> event) {
            LevelChunk chunk = event.getObject();
            ChunkSafetyProvider provider = new ChunkSafetyProvider(() -> chunk.setUnsaved(true));
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }

        @SubscribeEvent public static void load(ChunkEvent.Load event) {
            if (event.isNewChunk() && event.getLevel() instanceof ServerLevel && event.getChunk() instanceof LevelChunk chunk) {
                chunk.getCapability(ChunkSafetyCapability.INSTANCE)
                        .ifPresent(data -> data.setTrust(TerrainTrust.TRUSTED));
            }
        }
    }
}
